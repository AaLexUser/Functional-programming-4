(ns query
  (:require [common :as c]
            [clojure.string :as str])
  (:import [dev.langchain4j.model.ollama OllamaChatModel]
           [dev.langchain4j.store.embedding.pgvector PgVectorEmbeddingStore]
           [dev.langchain4j.service AiServices]
           [dev.langchain4j.rag.content.retriever EmbeddingStoreContentRetriever]
           [dev.langchain4j.model.chat ChatLanguageModel]
           [dev.langchain4j.model.input PromptTemplate]
           [dev.langchain4j.rag.query Query]
           [dev.langchain4j.data.message UserMessage]))

(def prompt-template
  "Answer the question based only on the following context:

{{context}}

---

Answer the question based on the above context: {{question}}
Note to answer in language of question.
Output format: terminal")


(defn get-chat-model []
  (-> (OllamaChatModel/builder)
      (.baseUrl c/llm-base-url)
      (.modelName "llama3.1")  ; matching Python implementation
      (.temperature 0.7)
      .build))

(defn create-content-retriever []
  (-> (EmbeddingStoreContentRetriever/builder)
      (.embeddingStore (c/get-embedding-store))
      (.embeddingModel (c/get-embedding-model))
      (.maxResults (int 5))
      (.minScore 0.75)
      .build))

(defn format-sources [results]
  (into [] (map (fn [result]
                  {:id (.metadata (.textSegment result))
                   :content (.text (.textSegment result))})
                results)))

(defn print-sources [results]
  (apply str
         "Sources:\n"
         (for [result results]
           (str "----\n"
                "ID: " (.getString (.metadata (.textSegment result)) "index") "\n"
                "Path: " (.getString (.metadata (.textSegment result)) "absolute_directory_path") "\n"
                "Content: " (.text (.textSegment result)) "\n"
                "----\n"))))

(defn query-rag [query-text]
  (let [retriever (create-content-retriever)
        query (Query/from (str/trim query-text))
        results (.retrieve retriever query)
        context (str/join "\n\n---\n\n" (map #(.text (.textSegment %)) results))
        prompt (-> (PromptTemplate/from prompt-template)
                   (.apply (java.util.HashMap. {"context" context
                                                "question" query-text})))
        ;; _ (println "PROMPT:\n" prompt)
        model (get-chat-model)
        response (.generate model [(UserMessage/from (.text prompt))])
        sources (format-sources results)]
    (println (format "Response: %s\n%s" (.text (.content response)) (print-sources results)))
    {:response (.text (.content response))
     :sources sources}))

(defn -main [& args]
  (when-let [query-text (first args)]
    (query-rag query-text)))

(comment
  (let [retriever (create-content-retriever)
        query (Query/from "Цель курса")]
    (.retrieve retriever query)))