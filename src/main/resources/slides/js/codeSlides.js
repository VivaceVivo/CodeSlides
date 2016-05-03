/*
 * Extensions to the reveal.js presentation framework for embedding executable code snippets.
 *
 * all <pre><code> ... </code></pre> - blocks are extended with an 'eval'-button and a result
 * textfield where the evaluationresult will be rendered after 'eval'
 *
 * Blocks that should be evaluated on startup need to set the "data-eval" attribute to "true".
 * Non-Scala Code needs to set the "lang" attribute. Supported Language: "java"
 */

var codes = document.getElementsByTagName("code")

for (var i=0, max=codes.length; i < max; i++) {
    var codeBlock = codes[i];
    var codeId = "snippet_" + i 
    var resultId = "result_" + codeId
    
    if(codeBlock.getAttribute("data-eval") == "off"){
        //
    }else{
    	var lang = codeBlock.getAttribute("lang")==null?"":codeBlock.getAttribute("lang");
        // modify block: 
        codeBlock.setAttribute("onclick", "document.getElementById('"+resultId+"').style.visibility='hidden';");
        codeBlock.className="scala";
        codeBlock.style.overflowY='scroll';
        codeBlock.style.maxHeight = '50%';
        codeBlock.setAttribute("contenteditable", true);
        codeBlock.setAttribute("data-trim", true);
        codeBlock.setAttribute("style", "font-size: 22px; background: rgba(40,40,40,0.7);");
        codeBlock.setAttribute("onblur", "javascript: evalScript('"+codeId+"', '"+resultId+"', '"+lang+"')");
        codeBlock.id=codeId
        
        // add result-DIV:
        var pre = document.createElement('pre');
        var newdiv = document.createElement('div');
        newdiv.class="scala";
        newdiv.style="background-color:white;color:black;visibility:hidden;font-size:60%;";
        newdiv.align="left";
        pre.id=resultId
        codeBlock.parentNode.parentNode.appendChild(pre)
        pre.appendChild(newdiv)
        
        // execute block on load if data-eval="true"
        if(codeBlock.getAttribute("data-eval") == "true"){
          evalScript(codeId, resultId, lang);
        }
    }
}

function evalScript(blockId, targetId, lang){
    var blk = document.getElementById(blockId);
    var code = blk.innerText || blk.textContent; // textContent Fix for FireFox. ';' required for multi statements!
     // http://api.jquery.com/jQuery.post/
     // Send the data using post
    $.post( "http://127.0.0.1:8080/eval" + lang, code ,
    function(data){
        var result = window.JSON.parse(data)
        //alert("Data: " + data + "\nStatus: " + result.result);
        var target = document.getElementById(targetId);
        if("Success" == result.result){
        	target.innerHTML =  result.console;
        	target.style.background="rgba(40,40,90,0.7)";
        }else {
        	target.innerHTML = "<b>" + result.result +"</b><br>" + result.console;
        	target.style.background="rgba(90,40,40,0.7)";
        }
        target.style.visibility='visible';
        target.style.height = '160px';
        target.style.overflowY='scroll';
      });
}