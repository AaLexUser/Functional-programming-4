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


(defn get-existing-ids [embedding-store]
  (let [existing-items (.findAll embedding-store)]
    (into #{} (map #(.id %) existing-items))))

(defn clear-database []
  (println "✨ Clearing Database")
  (let [embedding-store (get-embedding-store)]
    (.deleteAll embedding-store)))

(defn load-documents []
  (let [path-matcher (.getPathMatcher (FileSystems/getDefault) "glob:*.md")
        parser (TextDocumentParser.)
        documents (FileSystemDocumentLoader/loadDocuments data-path path-matcher parser)]
    (println (str "Loaded " (count documents) " documents."))
    documents))

(defn split-text [documents]
  (let [splitter (DocumentSplitters/recursive chunk-size chunk-overlap)
        chunks (reduce (fn [acc doc]
                         (concat acc (.split splitter doc)))
                       []
                       documents)]
    (println (str "Split " (count documents) " documents into " (count chunks) " chunks."))
    chunks))

(defn calculate-chunk-ids [chunks]
  (map (fn [chunk]
         (let [metadata (.metadata chunk)
               source (.get metadata "source")
               index (.get metadata "start_index")
               chunk-id (str source ":" index)]
           (.put metadata "id" chunk-id)
           chunk))
       chunks))

(defn populate-db [documents]
  (let [embedding-store (get-embedding-store)
        embedding-model (get-embedding-model)]
    (for [document documents]
      (let [ingestor (-> (EmbeddingStoreIngestor/builder) 
                         (.documentSplitter (DocumentSplitters/recursive chunk-size chunk-overlap))
                         (.textSegmentTransformer (reify java.util.function.Function
                                                  (apply [_ segment]
                                                    (TextSegment/from
                                                     (str (.metadata segment "file_name") "\n" (.text segment))
                                                     (.metadata segment)))))
                         (.embeddingModel embedding-model)
                         (.embeddingStore embedding-store)
                         .build)]
        (.ingest ingestor document)))))

(defn -main [& args]
  (let [cli-options [["-r" "--reset" "Reset the database"]]
        {:keys [options]} (parse-opts args cli-options)]
    (when (:reset options)
      (clear-database))
    (let [documents (load-documents)]
      (populate-db documents)
      (println "✨ Done populating database"))))

(comment 
  (let [documents (load-documents)]
    (for [document documents]
      (let [ingestor embedding-store]
        (.ingest ingestor document)))))

(def embedding-store
  (-> (EmbeddingStoreIngestor/builder)
      (.documentSplitter (DocumentSplitters/recursive chunk-size chunk-overlap))
      (.embeddingModel (get-embedding-model))
      (.embeddingStore (get-embedding-store))
      .build))
