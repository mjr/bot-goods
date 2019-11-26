package services;

public class Commands {
    public static final String commandInitChar = "/";
    public static final String NOTFOUND_COMMAND = "/notfound";
    public static final String START_COMMAND = commandInitChar + "start";
    public static final String HELP_COMMAND = commandInitChar + "help";
    public static final String CANCEL_COMMAND = commandInitChar + "cancel";

    public static final String newGoodCommand = commandInitChar + "newgood";
    public static final String searchGoodCommand = commandInitChar + "searchgood";
    public static final String listGoodsCommand = commandInitChar + "listgoods";
    public static final String deleteGoodCommand = commandInitChar + "deletegood";
    public static final String moveGoodCommand = commandInitChar + "movegood";

    public static final String newLocationCommand = commandInitChar + "newlocation";
    public static final String listLocationsCommand = commandInitChar + "listlocations";
    public static final String deleteLocationCommand = commandInitChar + "deletelocation";

    public static final String newGoodsCategoryCommand = commandInitChar + "newgoodscategory";
    public static final String listGoodsCategoriesCommand = commandInitChar + "listgoodscategories";
    public static final String deleteGoodsCategoryCommand = commandInitChar + "deletegoodscategory";

    public static final String reportCommand = commandInitChar + "report";
    public static final String reportFileCommand = commandInitChar + "reportfile";
}
