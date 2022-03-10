sudo wget -nc https://www.antlr.org/download/antlr-4.9.2-complete.jar
export CLASSPATH=".:/antlr-4.9.2-complete.jar:$CLASSPATH"
java -jar antlr-4.9.2-complete.jar -Dlanguage=Python3 -no-listener vyxal/VyxalLexer.g4 vyxal/VyxalParser.g4