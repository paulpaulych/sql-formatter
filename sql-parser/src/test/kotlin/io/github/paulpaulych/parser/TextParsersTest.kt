package io.github.paulpaulych.parser

import io.github.paulpaulych.TestUtils.err
import io.github.paulpaulych.TestUtils.ok
import io.github.paulpaulych.TestUtils.runParserTest
import io.github.paulpaulych.parser.TextParsers.flatMap
import io.github.paulpaulych.parser.TextParsers.regex
import io.github.paulpaulych.parser.TextParsers.scope
import io.github.paulpaulych.parser.TextParsers.string
import io.github.paulpaulych.parser.TextParsers.succeed
import io.github.paulpaulych.parser.TextParsersDsl.defer
import io.github.paulpaulych.parser.TextParsersDsl.or
import io.github.paulpaulych.parser.TextParsersDsl.and
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.data.*

internal class TextParsersTest : DescribeSpec({

    it("string parser") {
        runParserTest(
            row(string("aa"), "aa", ok("aa", consumed = 2)),
            row(string("aa"), "aaa", ok("aa", consumed = 2)),
            row(string(""), "aaa", ok("", consumed = 0)),
            row(string("ab"), "bbb", err(
                stack = listOf(
                    State("bbb", 0) to "expected",
                    State("bbb", 0) to "'ab'",
                )
            )),
            row(string("ab"), "aaa", err(
                stack = listOf(
                    State("aaa", 1) to "expected",
                    State("aaa", 1) to "'ab'",
                )
            )),
            row(string("aaa"), "aa", err(
                stack = listOf(
                    State("aa", 2) to "expected",
                    State("aa", 2) to "'aaa'",
                )
            )),
            row(string("aaa"), "bbb", err(
                stack = listOf(
                    State("bbb", 0) to "expected",
                    State("bbb", 0) to "'aaa'"
                )
            )),
            row(string("aaa"), "aab", err(
                stack = listOf(
                    State("aab", 2) to "expected",
                    State("aab", 2) to "'aaa'"
                )
            )),
        )
    }

    it("regex parser") {
        runParserTest(
            row(regex(Regex("\\d+")), "11aa", ok("11", consumed = 2)),
            row(regex(Regex("\\d+")), "11", ok("11", consumed = 2)),
            row(regex(Regex("\\d+")), "aa11", err(
                stack = listOf(
                    State("aa11", 0) to "expected",
                    State("aa11", 0) to "expression matching regex (\\d+)",
                )
            )),
            row(regex(Regex("\\d+")), "aa", err(
                stack = listOf(
                    State("aa", 0) to "expected",
                    State("aa", 0) to "expression matching regex (\\d+)"
                )
            )),
        )
    }

    it("succeeds parser") {
        runParserTest(
            row(succeed(2), "", ok(2, consumed = 0)),
            row(succeed("1235"), "aasdasdff", ok("1235", consumed = 0)),
        )
    }

    it("or parser") {
        runParserTest(
            row(string("abc") or { string("aaa") }, "abc", ok("abc", consumed = 3)),
            row(string("abc") or { string("aaa") }, "aaavvv", ok("aaa", consumed = 3)),
            row(regex(Regex("\\d+")) or { string("aaa") }, "11aa", ok("11", consumed = 2)),
            row(regex(Regex("\\d+")) or { string("aaa") }, "aaa", ok("aaa", consumed = 3)),
            row(string("bb") or { string("aa") }, "ccaabb", err(
                stack = listOf(
                    State("ccaabb", 0) to "expected",
                    State("ccaabb", 0) to "'aa'",
                )
            )),
        )
    }

    it("flatMap parser - context dependent parsers") {
        val alphabeticallyNext: (String) -> Parser<String> = { prev ->
            string(prev.first().inc().toString())
        }
        runParserTest(
            row(flatMap(string("a"), alphabeticallyNext), "abc", ok("b", consumed = 2)),
            row(flatMap(string("a"), alphabeticallyNext), "acb", err(
                stack = listOf(
                    State("acb", 1) to "expected",
                    State("acb", 1) to "'b'",
                )
            )),
            row(flatMap(string("b"), alphabeticallyNext), "acb", err(
                stack = listOf(
                    State("acb", 0) to "expected",
                    State("acb", 0) to "'b'",
                )
            )),
        )
    }

    it("scope combinator") {
        runParserTest(
            row(scope("greeting", string("hello, ") and string("world").defer()), "hello, world", ok(Pair("hello, ", "world"), consumed = 12)),
            row(scope("greeting", string("hello, ") and string("world").defer()), "hello, w0rld", err(
                stack = listOf(
                    State("hello, w0rld", 0) to "greeting",
                    State("hello, w0rld", 8) to "expected",
                    State("hello, w0rld", 8) to "'world'",
                )
            )),
        )
    }
})