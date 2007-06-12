@echo off
jar cvfm JDecker.jar manifest decker/*.class decker/model/*.class decker/util/*.class decker/view/*.class rulesets/*.txt rulesets/*.bmp rulesets/decker/*.txt rulesets/decker/artwork/*.bmp rulesets/decker/artwork/character_image/*.bmp rulesets/decker/artwork/home/*.bmp 
pause
