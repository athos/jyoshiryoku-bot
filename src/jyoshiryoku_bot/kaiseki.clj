(ns jyoshiryoku-bot.kaiseki
  (:import (org.atilika.kuromoji Token Tokenizer)))

(defn tokenize [text]
  (.tokenize (.build (Tokenizer/builder)) text))

(defn token-word [^Token token]
  (.trim (.getSurfaceForm token)))

(defn register-word [m word1 word2]
  (update-in m [word1 word2] (fnil inc 0)))

(defn load-text [filename]
  (->> (tokenize (slurp filename))
       (map token-word)
       (partition 2 1)
       (remove #(= (first %) ""))
       (reduce #(apply register-word %1 %2) {})))

(defn select-next-word [word-map word]
  (first (rand-nth (seq (get word-map word)))))

(defn create-sentence [word-map word]
  (loop [words [], word word]
    (if (and word (not= word "\""))
      (recur (conj words word) (select-next-word word-map word))
      (apply str words))))
