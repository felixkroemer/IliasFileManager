package com.felixkroemer.iliasfilemanager;

public class Constant {
	public enum ITEM_TYPE {FOLDER, FILE};
	public static final String LINK_PREFIX = "https://ilias.uni-mannheim.de/";
	public static final String MAINPAGE = "https://ilias.uni-mannheim.de/ilias.php?baseClass=ilPersonalDesktopGUI&cmd=jumpToSelectedItems";
	public static final String FOLDER_LINK_PATTERN_1 = "https://ilias\\.uni-mannheim\\.de/ilias\\.php\\?ref_id=\\d*&cmdclass=ilrepositorygui&cmdnode=vh&baseclass=ilrepositorygui";
	public static final String FOLDER_LINK_PATTERN_2 = "https://ilias\\.uni-mannheim\\.de/ilias\\.php\\?ref_id=\\d*&cmd=view&cmdclass=ilrepositorygui&cmdnode=vh&baseclass=ilrepositorygui";
	public static final String FILE_LINK_PATTERN = "https://ilias\\.uni-mannheim\\.de/goto\\.php\\?target=file_\\d*_download&client_id=ilias";
	public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:69.0) Gecko/20100101 Firefox/69.";
}
