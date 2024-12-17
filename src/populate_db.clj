(ns populate-db
   (:require [clojure.tools.cli :refer [parse-opts]]
             [common :refer [get-embedding-model get-embedding-store]])
   (:import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
            dev.langchain4j.store.embedding.EmbeddingStoreIngestor
            dev.langchain4j.data.document.splitter.DocumentSplitters
            dev.langchain4j.data.document.parser.TextDocumentParser
            java.nio.file.FileSystems
            dev.langchain4j.data.segment.TextSegment))

 (def data-path "data")
 (def chunk-size 500)
 (def chunk-overlap 200)


 (defn clear-database []
   (println "✨ Clearing Database")
   (let [embedding-store (get-embedding-store)]
     (.removeAll embedding-store)))

 (defn load-documents []
   (let [path-matcher (.getPathMatcher (FileSystems/getDefault) "glob:*.md")
         parser (TextDocumentParser.)
         documents (FileSystemDocumentLoader/loadDocuments data-path path-matcher parser)]
     (println (str "Loaded " (count documents) " documents."))
     documents))

 (defn add-documents []
   (let [documents (load-documents)]
     (doseq [document documents]
       (let [ingestor (-> (EmbeddingStoreIngestor/builder)
                          (.documentSplitter (DocumentSplitters/recursive chunk-size chunk-overlap))
                          (.embeddingModel (get-embedding-model))
                          (.embeddingStore (get-embedding-store))
                          .build)]
         (.ingest ingestor document)
         (println "✨ Done populating database")))))

 (defn -main [& args]
   (let [cli-options [["-r" "--reset" "Reset the database"]]
         {:keys [options]} (parse-opts args cli-options)]
     (when (:reset options)
       (clear-database))
     (add-documents)))