package vyxal

import scala.util.parsing.combinator._

case class VyxalCompilationError(msg: String)

enum VyxalToken {
  case Number(value: String)
  case Str(value: String)
  case StructureOpen(value: String)
  case StructureClose(value: String)
  case StructureAllClose
  case ListOpen
  case ListClose
  case Command(value: String)
  case Digraph(value: String)
  case MonadicModifier(value: String)
  case DyadicModifier(value: String)
  case TriadicModifier(value: String)
  case TetradicModifier(value: String)
  case SpecialModifier(value: String)
  case CompressedString(value: String)
  case CompressedNumber(value: String)
  case DictionaryString(value: String)
  case Comment(value: String)
  case GetVar(value: String)
  case SetVar(value: String)
  case Branch
  // Special intermediate composite tokens
  case CompositeNilad(value: List[VyxalToken])
  case CompositeMonad(value: List[VyxalToken])
  case CompositeDyad(value: List[VyxalToken])
}

import VyxalToken.*

val CODEPAGE = """ᵃᵇᶜᵈᵉᶠᶢᴴᶤᶨᵏᶪᵐⁿᵒᵖᴿᶳᵗᵘᵛᵂᵡᵞᶻᶴ′″‴⁴ᵜ !"#$%&'()*+,-./0123456789:;
<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ\\[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~¦ȦḂĊḊĖḞĠḢİĿṀṄ
ȮṖṘṠṪẆẊικȧḃċḋėḟġḣŀṁṅȯṗṙṡṫẋƒΘΦ§ẠḄḌḤỊḶṂṆỌṚṢṬ…≤≥≠₌⁺⁻⁾√∑«»⌐∴∵⊻₀₁₂₃₄₅₆₇₈₉λƛΩ₳µ∆øÞ½ʀɾ¯
×÷£¥←↑→↓±‡†Π¬∧∨⁰¹²³¤¨∥∦ı„”ð€“¶ᶿᶲ•≈¿ꜝ"""

val MONADIC_MODIFIERS = "ᵃᵇᶜᵈᵉᶠᶢᴴᶤᶨᵏᶪᵐⁿᵒᵖᴿᶳᵘᵛᵂᵡᵞᶻᶴ¿′/\\~v@`ꜝ"
val DYADIC_MODIFIERS = "″∥∦"
val TRIADIC_MODIFIERS = "‴"
val TETRADIC_MODIFIERS = "⁴"
val SPECIAL_MODIFIERS = "ᵗᵜ"

object Lexer extends RegexParsers {
  override def skipWhitespace = true

  def number: Parser[VyxalToken] = """(0(?=[^.ı])|\d+(\.\d*)?(\ı\d*)?)""".r ^^ {
    value => Number(value)
  }

  def string: Parser[VyxalToken] = """("(?:[^"„”“\\]|\\.)*["„”“])""".r ^^ {
    value =>
      // If the last character of each token is ", then it's a normal string
      // If the last character of each token is „, then it's a compressed string
      // If the last character of each token is ”, then it's a dictionary string
      // If the last character of each token is “, then it's a compressed number

      // So replace the non-normal string tokens with the appropriate token type

      // btw thanks to @pxeger and @mousetail for the regex
      val text = value.substring(1, value.length - 1).replaceAll("\\\\\"", "\"")

      value.charAt(value.length - 1) match {
        case '"' => Str(text)
        case '„' => CompressedString(text)
        case '”' => DictionaryString(text)
        case '“' => CompressedNumber(text)
        case _   => throw Exception("Invalid string")
      }
  }

  def singleCharString: Parser[VyxalToken] = """'.""".r ^^ { value =>
    Str(value.substring(1))
  }

  def structureOpen: Parser[VyxalToken] = """[\[\(\{λƛΩ₳µ]|#@""".r ^^ { value =>
    StructureOpen(value)
  }

  def structureClose: Parser[VyxalToken] = """[\}\)]""".r ^^ { value =>
    StructureClose(value)
  }

  def structureAllClose: Parser[VyxalToken] = """\]""".r ^^^ StructureAllClose

  def listOpen: Parser[VyxalToken] = """(#\[)|⟨""".r ^^^ ListOpen

  def listClose: Parser[VyxalToken] = """(#\])|⟩""".r ^^^ ListClose

  def digraph: Parser[VyxalToken] = s"[∆øÞ#k][$CODEPAGE]".r ^^ { value =>
    Digraph(value)
  }

  def command: Parser[VyxalToken] = s"[$CODEPAGE]".r ^^ { value =>
    Command(value)
  }

  def getVariable: Parser[VyxalToken] = """(\#\<)[0-9A-Za-z]*""".r ^^ { value =>
    GetVar(value.substring(2, value.length))
  }

  def setVariable: Parser[VyxalToken] = """(\#\>)[0-9A-Za-z]*""".r ^^ { value =>
    SetVar(value.substring(2, value.length))
  }

  def monadicModifier: Parser[VyxalToken] =
    s"""[$MONADIC_MODIFIERS]""".r ^^ { value => MonadicModifier(value) }

  def dyadicModifier: Parser[VyxalToken] =
    s"""[$DYADIC_MODIFIERS]""".r ^^ { value => DyadicModifier(value) }

  def triadicModifier: Parser[VyxalToken] =
    s"""[$TRIADIC_MODIFIERS]""".r ^^ { value => TriadicModifier(value) }

  def tetradicModifier: Parser[VyxalToken] =
    s"""[$TETRADIC_MODIFIERS]""".r ^^ { value => TetradicModifier(value) }

  def specialModifier: Parser[VyxalToken] =
    s"""[$SPECIAL_MODIFIERS]""".r ^^ { value => SpecialModifier(value) }

  def comment: Parser[VyxalToken] = """##[^\n]*""".r ^^ { value =>
    Comment(value)
  }

  def branch = "|" ^^^ Branch

  def tokens: Parser[List[VyxalToken]] = phrase(
    rep1(
      comment | branch | number | string | getVariable | setVariable | singleCharString | digraph
        | monadicModifier | dyadicModifier | triadicModifier | tetradicModifier
        | specialModifier | structureOpen | structureClose | structureAllClose
        | listOpen | listClose | command
    )
  )

  def apply(code: String): Either[VyxalCompilationError, List[VyxalToken]] = {
    (parse(tokens, code): @unchecked) match {
      case NoSuccess(msg, next)  => Left(VyxalCompilationError(msg))
      case Success(result, next) => Right(result)
    }
  }
}
