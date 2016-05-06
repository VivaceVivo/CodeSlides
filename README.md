# CodeSlides
Extension for Reveal.js to embed a Scala 'REPL' into slides

Basic usage:

> sbt run

Application will start on port 8080

To use this extension, you need to activate the javascript extension by
importing it into the slides:

&lt;script src="js/codeSlides.js"&gt;&lt;/script&gt;

All sections containing a code block are modified to be able to execute
the code in the Scala interpreter. Example:

&lt;section&gt;<br>
&lt;pre&gt;&lt;code&gt;<br>
println("Hello World!")<br>
&lt;pre&gt;&lt;code&gt;<br>
&lt;section&gt;

when the Code bock looses the focus (onblur), the content is sent to the
scripting backend for evaluation. The resulting text is then rendered in a
result div.

Have Fun!
