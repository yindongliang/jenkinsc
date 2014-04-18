/*
   Behaviour v1.1 by Ben Nolan, June 2005. Based largely on the work
   of Simon Willison (see comments by Simon below).

   Description:

   	Uses css selectors to apply javascript behaviours to enable
   	unobtrusive javascript in html documents.

   Usage:

        Behaviour.specify('b.someclass', 'myrules.alert', 10, function(element) {
            element.onclick = function() {
                alert(this.innerHTML);
            }
        });
        Behaviour.specify('#someid u', 'myrules.blah', 0, function(element) {
            element.onmouseover = function() {
                this.innerHTML = "BLAH!";
            }
        });

	// Call Behaviour.apply() to re-apply the rules (if you
	// update the dom, etc).

   License:

   	This file is entirely BSD licensed.

   More information:

   	http://ripcord.co.nz/behaviour/

*/

var Behaviour = (function() {
    var storage = [{selector: '', id: '_deprecated', priority: 0}];
    return {

    /**
     * Specifies something to do when an element matching a CSS selector is encountered.
     * @param {String} selector a CSS selector triggering your behavior
     * @param {String} id combined with selector, uniquely identifies this behavior; prevents duplicate registrations
     * @param {Number} priority relative position of this behavior in case multiple apply to a given element; lower numbers applied first (sorted by id then selector in case of tie); choose 0 if you do not care
     * @param {Function} behavior callback function taking one parameter, a (DOM) {@link Element}, and returning void
     */
    specify : function(selector, id, priority, behavior) {
        for (var i = 0; i < storage.length; i++) {
            if (storage[i].selector == selector && storage[i].id == id) {
                storage.splice(i, 1);
                break;
            }
        }
        storage.push({selector: selector, id: id, priority: priority, behavior: behavior});
        storage.sort(function(a, b) {
            var location = a.priority - b.priority;
            return location != 0 ? location : a.id < b.id ? -1 : a.id > b.id ? 1 : a.selector < b.selector ? -1 : a.selector > b.selector ? 1 : 0;
        });
    },

        /** @deprecated For backward compatibility only; use {@link specify} instead. */
	list : new Array,

        /** @deprecated For backward compatibility only; use {@link specify} instead. */
	register : function(sheet){
		Behaviour.list.push(sheet);
	},

	start : function(){
		Behaviour.addLoadEvent(function(){
			Behaviour.apply();
		});
	},

	apply : function(){
        this.applySubtree(document);
    },

    /**
     * Applies behaviour rules to a subtree/subforest.
     *
     * @param {HTMLElement|HTMLElement[]} startNode
     *      Subtree/forest to apply rules.
     *
     *      Within a single subtree, rules are the outer loop and the nodes in the tree are the inner loop,
     *      and sometimes the behaviour rules rely on this ordering to work correctly. When you pass a forest,
     *      this semantics is preserved.
     */
    applySubtree : function(startNode,includeSelf) {
        if (!(startNode instanceof Array)) {
            startNode = [startNode];
        }
        storage._each(function (registration) {
            if (registration.id == '_deprecated') {
                Behaviour.list._each(function(sheet) {
                    for (var selector in sheet){
                        startNode._each(function (n) {
                            var list = findElementsBySelector(n, selector, includeSelf);
                            if (list.length > 0) { // just to simplify setting of a breakpoint.
                                //console.log('deprecated:' + selector + ' on ' + list.length + ' elements');
                                list._each(sheet[selector]);
                            }
                        });
                    }
                });
            } else {
                startNode._each(function (node) {
                    var list = findElementsBySelector(node, registration.selector, includeSelf);
                    if (list.length > 0) {
                        //console.log(registration.id + ':' + registration.selector + ' @' + registration.priority + ' on ' + list.length + ' elements');
                        list._each(registration.behavior);
                    }
                });
            }
        });
    },

    addLoadEvent : function(func){
		var oldonload = window.onload;

		if (typeof window.onload != 'function') {
			window.onload = func;
		} else {
			window.onload = function(e) {
				oldonload(e);
				func(e);
			}
		}
	}
}})();

// by ydl
Behaviour.addLoadEvent(function(){
	
	var elems=document.getElementsBySelector("div[class=section-header]");
	for(var i=0;i<elems.length;i++){
		
		var idx=elems[i].innerHTML.lastIndexOf("</a>Build");
		var idx2=elems[i].innerHTML.lastIndexOf("</a>ビルド");
		if(elems[i].innerHTML=="Build"
			||(idx!=-1&&idx==elems[i].innerHTML.length-9)
			||elems[i].innerHTML=="ビルド"
				||(idx2!=-1&&idx2==elems[i].innerHTML.length-7)){
			
			var div=document.createElement("div");
			div.id="iconzone";

			div.innerHTML="<a href='javascript:void(0)'  onclick='popUp()'>build ant assistant</a>";

			elems[i].appendChild(div);
		}
	}
	
});

function popUp() {
	var ps=document.getElementsByName("_.properties");
	if(ps==null||ps.length==0){
		
		alert("please open build ant area first!");
		return;
	}
	
//	window.showModalDialog(
//			"http://cdcjp03.cn.oracle.com:8085/getBulidProperties/anthelper/1",
//			window,
//			"dialogWidth=1200px;dialogHeight=750px");
	
	
	var loc=window.location.href;
	var idx=loc.indexOf("/",7);
	loc=loc.substring(0, idx);
	//value="▼" onclick="expandTextArea(this,'textarea._.properties')"
	for(var i=0;i<ps.length;i++){

		var evt = document.createEvent("MouseEvents");
		evt.initEvent("click", false, false);
		ps[i].parentNode.nextSibling.children[0].dispatchEvent(evt);

		
	}
	
		
	window.showModalDialog(
			loc+"/getBulidProperties/anthelper/1",
			window,
			"dialogWidth=1200px;dialogHeight=750px");
	}


//by ydl
Behaviour.start();

/*
   The following code is Copyright (C) Simon Willison 2004.

   document.getElementsBySelector(selector)
   - returns an array of element objects from the current document
     matching the CSS selector. Selectors can contain element names,
     class names and ids and can be nested. For example:

       elements = document.getElementsBySelect('div#main p a.external')

     Will return an array of all 'a' elements with 'external' in their
     class attribute that are contained inside 'p' elements that are
     contained inside the 'div' element which has id="main"

   New in version 0.4: Support for CSS2 and CSS3 attribute selectors:
   See http://www.w3.org/TR/css3-selectors/#attribute-selectors

   Version 0.4 - Simon Willison, March 25th 2003
   -- Works in Phoenix 0.5, Mozilla 1.3, Opera 7, Internet Explorer 6, Internet Explorer 5 on Windows
   -- Opera 7 fails
*/

function findElementsBySelector(startNode,selector,includeSelf) {
    if(includeSelf) {
        function isSelfOrChild(c) {
          while(true) {
              if(startNode == c) return true;
              if(c == null) return false;
              c = c.parentNode;
          }
        }
        return Prototype.Selector.select(selector, startNode.parentNode).filter(isSelfOrChild);
    } else {
        return Prototype.Selector.select(selector, startNode);
    }
}

document.getElementsBySelector = function(selector) {
    return findElementsBySelector(document,selector);
}

/* That revolting regular expression explained
/^(\w+)\[(\w+)([=~\|\^\$\*]?)=?"?([^\]"]*)"?\]$/
  \---/  \---/\-------------/    \-------/
    |      |         |               |
    |      |         |           The value
    |      |    ~,|,^,$,* or =
    |   Attribute
   Tag
*/
