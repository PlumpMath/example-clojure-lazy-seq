(ns examples-lazy-seq.core
  (:use [clojure.pprint :only [pprint]]))

(defn -main
  "I don't do a whole lot."
  [& args]

  ;; TODO:
  ;;  Lazy sequence 설명.

  (let [random (fn random []
                 (lazy-seq
                   (cons (rand-int 100) (random))))]

    ;; 위 "random" 함수는 조건없이 재귀호출을 한다.
    ;; 즉, 이 함수를 호출하면 끝나지 않거나 스택을 모두 소진하여 예외를 발생시킨다.
    ;; 하지만 "random" 함수를 호출해도 위에서 언급한 결과는 발생하지 않는다.
    ;; 이는 "lazy-seq"가 자신의 몸통에 포함된 표현식을 지연평가(lazy evaluation) 시키기 때문이다.

    ;; REPL에서 "random" 함수를 호출하면, 그 결과를 보여주기 위해서 바로 평가하기 때문에
    ;; 아래와 같이 random-result 변수에 "random" 함수의 결과를 담는 방식으로
    ;; "random" 함수를 호출하였다.

    (def random-result (random))

    ;; 이제, "lazy-seq"는 어떻게 구현되어 있는지 살펴보자!
    ;; 아래와 같이 "lazy-seq" 매크로를 확장해 보면,
    ;; "lazy-seq"의 몸통을 람다함수로 감싸 LazySeq 클래스의 파라메터로 전달한다.
    ;; 즉, 평가를 지연하기 위해 "lazy-seq" 몸통을 람다함수로 감싸고 LazySeq 클래스에게 위임한다.

    (macroexpand '(lazy-seq (cons (rand-int 100) (random))))
    ;;=> (new clojure.lang.LazySeq (fn* [] (cons (rand-int 100) (random))))

    ;; 그러면 LazySeq 클래스는 주어진 람다함수를 언제 수행(invoke)시킬까?
    ;; LazySeq 클래스는 주어진 람다함수를 보관하고 있다가 LazySeq.seq 메서드가 호출되면 수행시킨다.
    ;; 아래와 같이 seq 함수로 LazySeq.seq 메서드를 간접적으로 호출하면,
    ;; "lazy-seq"의 몸통이 실행되는 것을 불수 있다.

    ;; 참고로 위 "random" 함수의 경우 평가되면 무한 재귀호출을 하기 때문에
    ;; 아래와 같은 방식으로 호출하면 안된다.

    ;; 그리고 LazySeq 클래스는 'clojure/src/jvm/clojure/lang/LazySeq.java',
    ;; RT.seq 메서드는 'clojure/src/jvm/clojure/lang/RT.java'에 정의되어 있다.

    (def test-eval (println "evaluation"))
    ;;=> "evaluation"
    (def test-lazy-eval (lazy-seq (do (println "evaluation") nil)))
    (seq test-lazy-eval)
    ;;=> "evaluation"
    ;;=> nil

    ;; 여태껏 설명한 내용을 정리하자면, "lazy-seq" 매크로는 주어진 몸통을 람다함수로 감싸
    ;; LazySeq 클래스의 인스턴스를 생성할 때 파라메터로 넘긴다.
    ;; 이 람다함수는 LazySeq.seq 메서드가 호출될 때 호출되고, seq 메서드는 그 결과를 반환한다.

    ;; 그리고 LazySeq.seq 메서드는 ISeq 타입을 반환하므로
    ;; "lazy-seq"의 몸통은 ISeq 타입으로 변환 가능한 값을 반환해야 한다.
    ;; 참고로 변경가능한 타입들은 RT.seq와 RT.seqFrom 메서드에 정의되어 있다.

    (seq (lazy-seq '(1 2 3)))
    ;;=> (1 2 3)
    (seq (lazy-seq 1))
    ;;=> IllegalArgumentException Don't know how to create ISeq from: java.lang.Long
    ;;=>  clojure.lang.RT.seqFrom (RT.java:505)

    ;; "lazy-seq"가 어떻게 무한대 시퀀스를 생성하는 과정을 자세히 살펴보기 위해
    ;; 위 "random" 함수를 다시 살펴보자!

    #_(fn random []
        (lazy-seq
          (cons (rand-int 100) (random))))

    ;; 먼저, 시퀀스를 생성하기 위해서 "cons" 함수를 사용한다.
    ;; "cons" 함수의 주요한 특징은 주어진 시퀀스를 평가하지 않는다는 점이다.
    ;; TODO:
    ;;  Cons 클래스 설명.

    (def test-cons-func (cons 10 (lazy-seq (do (println "evaluation") '(20)))))
    (seq test-cons-func)
    ;;=> "evaluation"
    ;;=> (10 20)

    ))
