package io.github.paulpaulych.parser

import io.github.paulpaulych.parser.TextParsers.flatMap
import io.github.paulpaulych.parser.TextParsers.or
import io.github.paulpaulych.parser.TextParsers.regex
import io.github.paulpaulych.parser.TextParsers.succeed
import java.util.regex.Pattern

object TextParsersDsl {

    fun <A> Parser<A>.defer(): () -> Parser<A> = { this }

    fun <A> Parser<A>.many(): Parser<List<A>> {
        val notEmptyList = map2(this, { this.many() }) { a, la -> listOf(a) + la }
        return notEmptyList or { succeed(listOf()) }
    }

    fun <A, B> Parser<A>.flatMap(f: (A) -> Parser<B>): Parser<B> {
        return flatMap(this, f)
    }

    infix fun <A> Parser<out A>.or(pb: () -> Parser<out A>): Parser<A> {
        return or(this, pb)
    }

    fun <A, B> Parser<A>.map(f: (A) -> B): Parser<B> {
        return this.flatMap { a -> succeed(f(a)) }
    }

    fun <A, B, C> map2(
        pa: Parser<A>,
        pb: () -> Parser<B>,
        f: (A, B) -> C
    ): Parser<C> {
        return pa.flatMap { a ->
            pb().map { b -> f(a, b)  }
        }
    }

    infix fun <A, B> Parser<A>.and(pb: () -> Parser<B>): Parser<Pair<A, B>> = map2(this, pb) { a, b -> Pair(a, b) }

    infix fun <A, B> Parser<A>.and(pb: Parser<B>): Parser<Pair<A, B>> = map2(this, pb.defer()) { a, b -> Pair(a, b) }

    operator fun <A, B> Parser<A>.plus(pb: Parser<B>): Parser<Pair<A, B>> = map2(this, pb.defer()) { a, b -> Pair(a, b) }

    operator fun <A, B> Parser<A>.plus(pb: () -> Parser<B>): Parser<Pair<A, B>> = map2(this, pb) { a, b -> Pair(a, b) }

    infix fun <A, B> Parser<A>.skipR(p: Parser<B>): Parser<A> =
        (this and { p }).map { it.first }

    infix fun <A, B> Parser<A>.skipL(p: Parser<B>): Parser<B> =
        (this and { p }).map { it.second }

    infix fun <A, B> Parser<A>.skipL(p: () -> Parser<B>): Parser<B> =
        (this and p).map { it.second }

    fun <A> surround(
        start: Parser<*>,
        stop: Parser<*>,
        parser: () -> Parser<A>
    ): Parser<A> = start skipL parser skipR stop

    fun <A> surround(
        start: Parser<*>,
        stop: Parser<*>,
        parser: Parser<A>
    ): Parser<A> = surround(start, stop, parser.defer())

    fun thru(s: String): Parser<String> =
        regex(Regex(".*?${Pattern.quote(s)}"))

    infix fun <A> Parser<A>.sepBy(sep: Parser<String>): Parser<List<A>> {
        val notEmptyList = map2(this, { (sep skipL this).many() }) { a, la ->
            listOf(a) + la
        }
        return notEmptyList or { succeed(listOf()) }
    }

    infix fun <A> Parser<A>.sepBy1(sep: Parser<String>): Parser<List<A>> {
        val notEmptyList = map2(this, { (sep skipL this).many() }) { a, la ->
            listOf(a) + la
        }
        return notEmptyList
    }
}