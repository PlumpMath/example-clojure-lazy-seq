(ns examples-lazy-seq.core
  (:use [clojure.pprint :only [pprint]]))

(defn -main
  "I don't do a whole lot."
  [& args]

  ;; TODO:
  ;;  Lazy sequence 설명.

  (def random (fn random []
                (lazy-seq
                  (cons (rand-int 100) (random)))))

  ;; 위 "random" 함수는 조건없이 재귀호출을 한다.
  ;; 즉, 이 함수를 호출하면 끝나지 않거나 스택을 모두 소진하여 예외를 발생시킨다.
  ;; 하지만, 아래와 같이 "random" 함수를 호출해도 위에서 언급한 결과는 발생하지 않는다.
  ;; 이는 lazy-seq가 자신의 몸통의 표현식을 지연평가(lazy evaluation) 시키기 때문이다.

  ;; REPL에서 "random" 함수를 호출하면, 그 결과를 보여주기 위해 바로 평가하기 때문에-
  ;; 아래와 같이 random-result 변수에 "random" 함수의 결과를 담는 방식으로 "random" 함수를 호출하였다.

  (def random-result (random))

  ;; 아래와 같이 lazy-seq 매크로를 풀어보면 몸통을 lambda로 감싸 LazySeq 클래스의 파라메터로 전달한다.
  ;; 즉, lazy-seq는 평가를 지연시키기 위해 몸통의 표현식을 lambda로 감싸고-
  ;; LazySeq 클래스에게 이 lambda의 실행을 위임한다.

  (macroexpand '(lazy-seq (cons (rand-int 100) (random))))
  ;;=> (new clojure.lang.LazySeq (fn* [] (cons (rand-int 100) (random))))

  ;; LazySeq 클래스는 주어진 lambda를 보관하고 있다가 LazySeq.seq 메서드가 호출되면 수행시킨다.
  ;; 아래와 같이 seq 함수로 LazySeq.seq 메서드를 간접적으로 호출하면,
  ;; lazy-seq의 몸통이 실행되는 것을 불수 있다.

  (def test-eval (println "evaluation"))
  ;;=> "evaluation"
  (def test-lazy-eval (lazy-seq (do (println "evaluation") nil)))
  (seq test-lazy-eval)
  ;;=> "evaluation"
  ;;=> nil

  ;; 즉, lazy-seq 매크로는 자신의 몸통을 lambda로 감싸 LazySeq 클래스의 인스턴스를 생성할 때 파라메터로 넘긴다.
  ;; 이 lambda는 LazySeq.seq 메서드가 호출될 때 평가되고, 이 메서드는 lambda의 결과를 반환한다.
  ;; LazySeq.seq 메서드는 ISeq 타입을 반환하므로 lazy-seq의 몸통의 결과는 ISeq 타입으로 변환 가능해야 한다.
  ;; 참고로 변경가능한 타입들은 RT.seq와 RT.seqFrom 메서드에 정의되어 있다.

  (seq (lazy-seq '(1 2 3)))
  ;;=> (1 2 3)
  (seq (lazy-seq 1))
  ;;=> IllegalArgumentException Don't know how to create ISeq from: java.lang.Long
  ;;=>  clojure.lang.RT.seqFrom (RT.java:505)

  #_(fn random []
      (lazy-seq
        (cons (rand-int 100) (random))))

  ;; "random" 함수를 다시 자세히 살펴보면,
  ;; 끝나는 조건이 없는 재귀 함수이기 때문에 평가되면 무한히 호출될 것 같지만,
  ;; 아래와 같이 평가(호출)되어도 무한히 호출되지 않는다.

  (take 10 (random))
  ;;=> (6 22 51 16 38 93 31 7 81 91)

  ;; 아래 "bad-random" 함수의 결과를 보면, take 함수 때문은 아닌 것 같다.
  ;; "bad-random" 함수를 무한히 호출해 스택이 폭발해버렸다.

  (let [bad-random (fn random []
                     (lazy-seq
                       (conj (vector (rand-int 100)) (random))))]
    (take 10 (bad-random)))
  ;;=> StackOverflowError  ...

  ;; 위 "random" 함수에서는 시퀀스를 생성하기 위해 cons 함수를 사용하는데,
  ;; 아래에서 볼수 있듯이 cons 함수는 바로 시퀀스를 생성하지 않는다.

  ;; TODO:
  ;;  Cons 클래스가 리스트를 만드는 방법 설명.
  )
