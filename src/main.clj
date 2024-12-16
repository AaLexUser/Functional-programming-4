(ns main
  (:import java.util.Date
           java.util.Calendar))

(defn today-string []
  (let [c (Calendar/getInstance)
        year (.get c Calendar/YEAR)
        month (.get c Calendar/MONTH)
        day (.get c Calendar/DATE)]
    (str year "-" (if (< month 10) (str "0" month) month) "-" (if (< day 10) (str "0" day) day))))

(today-string)