
(set-env!
 :dependencies '[[org.clojure/clojurescript "1.9.216"     :scope "test"]
                 [org.clojure/clojure       "1.8.0"       :scope "test"]
                 [adzerk/boot-cljs          "1.7.228-1"   :scope "test"]
                 [adzerk/boot-reload        "0.4.12"      :scope "test"]
                 [cirru/stack-server        "0.1.8"       :scope "test"]
                 [adzerk/boot-test          "1.1.2"       :scope "test"]
                 [mvc-works/hsl             "0.1.2"]
                 [respo/ui                  "0.1.1"]
                 [cirru/editor              "0.1.12"]
                 [respo                     "0.3.9"]])

(require '[adzerk.boot-cljs   :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[stack-server.core  :refer [start-stack-editor! transform-stack]]
         '[respo.alias        :refer [html head title script style meta' div link body]]
         '[respo.render.static-html :refer [make-html]]
         '[adzerk.boot-test   :refer :all]
         '[clojure.java.io    :as    io])

(def +version+ "0.1.0")

(task-options!
  pom {:project     'cirru/org
       :version     +version+
       :description "Cirru home page"
       :url         "https://github.com/Cirru/cirru.org"
       :scm         {:url "https://github.com/Cirru/cirru.org"}
       :license     {"MIT" "http://opensource.org/licenses/mit-license.php"}})

(defn use-text [x] {:attrs {:innerHTML x}})
(defn html-dsl [data fileset]
  (make-html
    (html {}
    (head {}
      (title (use-text "Cirru: an editor for AST"))
      (link {:attrs {:rel "icon" :type "image/png" :href "http://repo.cirru.org/logo.cirru.org/cirru-400x400.png"}})
      (link {:attrs {:href "https://fonts.googleapis.com/css?family=Hind+Vadodara|Josefin+Sans:300,400" :rel "stylesheet"}})
      (if (:build? data)
        (link (:attrs {:rel "manifest" :href "manifest.json"})))
      (meta'{:attrs {:charset "utf-8"}})
      (meta' {:attrs {:name "viewport" :content "width=device-width, initial-scale=1"}})
      (style (use-text "body {margin: 0;}"))
      (style (use-text "body * {box-sizing: border-box;}"))
      (if (:build? data)
        (div {:attrs {:innerHTML (slurp "html/ga.html")}}))
      (script {:attrs {:id "config" :type "text/edn" :innerHTML (pr-str data)}}))
    (body {}
      (div {:attrs {:id "app"}})
      (script {:attrs {:src "main.js"}})))))

(deftask html-file
  "task to generate HTML file"
  [d data VAL edn "data piece for rendering"]
  (with-pre-wrap fileset
    (let [tmp (tmp-dir!)
          out (io/file tmp "index.html")]
      (empty-dir! tmp)
      (spit out (html-dsl data fileset))
      (-> fileset
        (add-resource tmp)
        (commit!)))))

(deftask dev! []
  (set-env!
    :asset-paths #{"assets"})
  (comp
    (watch)
    (start-stack-editor!)
    (target :dir #{"src/"})
    (html-file :data {:build? false})
    (reload :on-jsload 'cirru-org.core/on-jsload
            :cljs-asset-path ".")
    (cljs)
    (target)))

(deftask generate-code []
  (comp
    (transform-stack :filename "stack-sepal.ir")
    (target :dir #{"src/"})))

(deftask build-advanced []
  (set-env!
    :asset-paths #{"assets"})
  (comp
    (transform-stack :filename "stack-sepal.ir")
    (cljs :optimizations :advanced)
    (html-file :data {:build? true})
    (target)))

(deftask rsync []
  (with-pre-wrap fileset
    (sh "rsync" "-r" "target/" "tiye.me:repo/Cirru/cirru.org" "--exclude" "main.out" "--delete")
    fileset))

(deftask build []
  (comp
    (transform-stack :filename "stack-sepal.ir")
    (pom)
    (jar)
    (install)
    (target)))

(deftask deploy []
  (set-env!
    :repositories #(conj % ["clojars" {:url "https://clojars.org/repo/"}]))
  (comp
    (build)
    (push :repo "clojars" :gpg-sign (not (.endsWith +version+ "-SNAPSHOT")))))

(deftask watch-test []
  (set-env!
    :source-paths #{"src" "test"})
  (comp
    (watch)
    (test :namespaces '#{cirru-org.test})))
