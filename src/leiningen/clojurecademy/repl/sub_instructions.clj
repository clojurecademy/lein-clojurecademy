(ns leiningen.clojurecademy.repl.sub-instructions
  (:require [leiningen.clojurecademy.util :as util]))


(defn- take-while-and-n-more
  [pred n coll]
  (let [[head tail] (split-with pred coll)]
    (concat head (take n tail))))

(defn- get-sub-instruction
  [name sub-instructions]
  (some #(when (= name (:name %)) %) sub-instructions))


(defn- get-sub-ins
  [route]
  (let [instruction (-> route :subject :instruction)
        route-vec   (:route-vec route)]
    (if (:run-pre-tests? instruction)
      (take-while-and-n-more #(not= (last route-vec) (:name %)) 1 (-> route :subject :instruction :sub-instructions))
      [(get-sub-instruction (last route-vec) (:sub-instructions instruction))])))


(defn get-err-msg-form-for-macro
  [test]
  `(case ~(:error-message-type (:is test))

     :none (format "%s\n"
                   ~(:error-message (:is test)))

     :simple (format "%s\n"
                     (or ~(:error-message (:is test)) "This assertion does not return true!"))

     :advanced (format "%s -> %s\n"
                       (quote ~(:form (:is test)))
                       (or ~(:error-message (:is test)) "This assertion does not return true!"))))


;;TODO refactor here
(defn- get-form
  [test]
  `(let [f#      '~(first (read-string (:form (:is test))))
         macro?# (util/macro? f#)]
     (if macro?#
       (if ~(read-string (:form (:is test)))
         true
         (util/runtime-ex ~(get-err-msg-form-for-macro test)))
       (let [form-eval# (util/form-eval f# [~@(drop 1 (read-string (:form (:is test))))])]
         (if (:result form-eval#)
           true
           (util/runtime-ex (case ~(:error-message-type (:is test))

                              :none (format "%s\n"
                                            ~(:error-message (:is test)))

                              :simple (format "%s -> %s\n"
                                              (:form form-eval#)
                                              (or ~(:error-message (:is test)) "This assertion does not return true!"))

                              :advanced (format "%s => %s -> %s\n"
                                                (quote ~(:form (:is test)))
                                                (:form form-eval#)
                                                (or ~(:error-message (:is test)) "This assertion does not return true!")))))))))


(defn get-sub-ins-tests-execution-ds
  [test-var route]
  (util/println-debug "*Getting sub instructions and starting tests...*\n")
  (let [route-vec        (:route-vec route)
        run-pre-tests?   (-> route :subject :instruction :run-pre-tests?)
        test-var-meta    (meta test-var)
        test-var-name    (:name test-var-meta)
        test-var-file    (:file test-var-meta)
        test-var-line    (:line test-var-meta)
        sub-instructions (get-sub-ins route)]
    (when run-pre-tests?
      (util/println-debug "*Run pre tests: `true`*\n"))
    (util/list-gen '(every? true?)
                   (cons 'list (map (fn [test]
                                      `(try
                                         ~(get-form test)
                                         (catch Exception e#
                                           (util/println-error
                                             (format "\nTest failed: `%s` - route: %s\n`defcoursetest` file: %s - Line: %d\n\n  Error: %s\n\n"
                                                     '~test-var-name
                                                     (if ~run-pre-tests?
                                                       (conj (vec (drop-last '~route-vec)) (quote ~(:name (:sub-ins test))))
                                                       '~route-vec)
                                                     ~test-var-file
                                                     ~test-var-line
                                                     (.getMessage e#)))
                                           false)))
                                    (for [sub-ins sub-instructions
                                          is      (:is (:testing sub-ins))]
                                      {:is      is
                                       :sub-ins sub-ins}))))))