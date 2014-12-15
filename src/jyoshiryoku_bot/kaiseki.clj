(ns jyoshiryoku-bot.kaiseki
  (:import (org.atilika.kuromoji Token Tokenizer)))

(defn tokenize [text]
  (.tokenize (.build (Tokenizer/builder)) text))

(defn token-word [token]
  (.trim (.getSurfaceForm token)))

(defn register-word [m word1 word2]
  (update-in m [word1 word2] (fnil inc 0)))

(defn load-text [filename]
  (->> (tokenize (slurp filename))
       (map token-word)
       (partition 2 1)
       (remove #(= (first %) ""))
       (reduce #(apply register-word %1 %2) {})))

(defn select-word [word-map]
  (first (rand-nth (seq word-map))))

(defn select-next-word [word-map word]
  (let [next-word-map (get word-map word)]
    (select-word next-word-map)))

(defn create-sentence [word-map word]
  (loop [sentence ""
         word word]
    (if word
      (if (= word "\"")
        sentence
        (recur (str sentence word) (select-next-word word-map word)))
      sentence)))
