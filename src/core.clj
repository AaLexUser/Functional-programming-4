(ns core
  (:require [query :as q]
           [populate-db :as p]
           [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(def cli-options
  [["-h" "--help" "Show help"]
   ["-r" "--reset" "Reset the database"]
   ["-q" "--query QUERY" "Query the database with the given query string"
    :validate [#(not (empty? %)) "Query cannot be empty"]]
   ["-p" "--populate" "Populate the database with documents"]])

(defn usage [options-summary]
  (->> ["RAG-based Q&A system for course materials"
        ""
        "Usage: java -jar app.jar [options] [query-string]"
        ""
        "Options:"
        options-summary
        ""
        "Examples:"
        "  java -jar app.jar -p                 # Populate the database"
        "  java -jar app.jar -q \"What is FP?\"   # Query the database"
        "  java -jar app.jar -r                 # Reset the database"]
       (clojure.string/join \newline)))

(defn -main [& args]
  (let [{:keys [options arguments summary errors]} (parse-opts args cli-options)]
    (cond
      (:help options)
      (do (println (usage summary))
          (System/exit 0))
      
      errors
      (do (println "Errors:")
          (doseq [error errors]
            (println error))
          (System/exit 1))
      
      (:reset options)
      (do (p/clear-database)
          (println "✨ Done resetting database"))
      
      (:query options)
      (do (q/query-rag (:query options))
          (println "✨ Done querying database"))
      
      (:populate options)
      (do (p/add-documents)
          (println "✨ Done populating database"))
      
      :else
      (do (println (usage summary))
          (System/exit 1)))))