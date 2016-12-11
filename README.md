# ImgReorder
Java application that can be used to reorder image files in a folder.
It present preview of the images in a panel.
These preview can be easily reordered. Then (when Apply button is pressed), the image files are renamed (a prefix is
added to the previous name, or replaced if it already start with "xxxx_" where xxxx is a number).


Usage :

Image in the panel can be selected by left click (selection allowed) and moveat a specific place by left click.
Selected images can also be removed by pressing "delete" key.
Images files are really renamed or deleted only when the "Apply" button is pressed (so reopen the folder or exit the application if you have done something wrong).


Comments :

This application is not well designed or commented (but I wrote only two classes).
It has just been built to respond to a specific personal one time need with efficiency.


Licence :

This application used :
- a class "WrapLayout" taken from http://tips4java.wordpress.com/2008/11/06/wrap-layout/ and originally written by Rob Camick.
- the Java image-scaling library "imgscalr" you can found here : http://www.thebuzzmedia.com/software/imgscalr-java-image-scaling-library/
with source code on git-hub here : https://github.com/thebuzzmedia/imgscalr under "Apache 2 License".
- A alphanum sorting algorithm under GNU Lesser General Public License taken from http://www.davekoelle.com/alphanum.html

Free to use, modify and distribute.
