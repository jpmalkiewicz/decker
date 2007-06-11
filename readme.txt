Decker (Java port)
version : 77%



NOT PLAYABLE YET



This is the unfinished Java port of the game Decker.
The road map lists the unfinished parts.
Requires Sun's JRE 5 to run. Get it from
http://java.com


See the decker project page for more information about the game :
http://sourceforge.net/projects/decker/


The port is not 1:1.
The main differences between Decker 1.9 and Decker 1.9.1 (Java port) will be :
- the game now has a scripting language for missions
- most message boxes have been turned into tooltips
- it is possible to have several instances of the same program running
  at the same time during a run
- support of custom decker sprites
- localization (english and german will be the first localizations)
- 100% Pure Java, so the program should run on almost every PC
- it is possible to accept several missions at the same time and to drop them again

- the level of the offered missions is based on the levels of the missions
  the player completes or fails. reputation and lifestyle are only used for the
  upper limit of the mission ratings for generic missions. lifestyle still
  determines the maximum level of shop items
- if a mission has IO nodes as its target, the probability of a given IO node
  type being chosen as the target differs slightly from the Decker 1.12 values
- SAAB (Manufacturing) occurred twice in the list of target corporations
  for generic runs. The second entry was replaced by Mattel (Consumer).
- Zurich Orbital Gemeinschaft Bank in the list of target corporations
  for generic runs has been changed to Zuerich Orbital Gemeinschafts-Bank


THE LICENSE
===========
Everything in this .zip file is protected by the GNU General Public License 2.0 (GPL).
The license text is included in the download (filename : gpl.txt).



ROAD MAP
========

80 %
- the program can generate systems   70% done
- the matrix view
- player can do missions
- mission debriefing works


90 %
- sound is working
- options are available
- improvement : a table view for choosing matrix icons, support of custom sprites


version 1.9.1
- add system scripting to the mission scripting language
- the program can load and save games in its own format
- the program can import save games in the decker 1.9 format
- german localization


future tasks
- add syntax highlighting to the Script Checker
- the skin can be changed from within the program
- the free matrix
- the mission editor
- the mission editor can generate decker systems from FasTrix systems



FILES CONTAINED IN THIS DOWNLOAD
================================
The following files should be there :
decker.jar                     the executable game and the source code
readme.txt
gpl.txt                        the license text
ScriptChecker.bat              starts the script checker
