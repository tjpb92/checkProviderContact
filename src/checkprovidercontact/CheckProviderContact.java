package checkprovidercontact;

import bdd.Ftoubib;
import bdd.FtoubibDAO;
import bdd.Furgent;
import bdd.FurgentDAO;
import bkgpi2a.HttpsClient;
import bkgpi2a.Identifiants;
import bkgpi2a.ProviderContact;
import bkgpi2a.WebServer;
import bkgpi2a.WebServerException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Footer;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PaperSize;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import utils.ApplicationProperties;
import utils.DBManager;
import utils.DBServer;
import utils.DBServerException;
import utils.Md5;

/**
 * Programme permettant de vérifier la cohérence entre la base de données métier
 * et la base de données de l'Extranet. Selon le mode de fonctionnement, les
 * corrections peuvent être effectuées.
 *
 * @author Thierry Baribaud
 * @version 0.02
 */
public class CheckProviderContact {

    /**
     * webServerType : prod pour le serveur de production, pre-prod pour le
     * serveur de pré-production. Valeur par défaut : pre-prod.
     */
    private String webServerType = "pre-prod";

    /**
     * dbServerType : prod pour le serveur de production, pre-prod pour le
     * serveur de pré-production. Valeur par défaut : pre-prod.
     */
    private String dbServerType = "pre-prod";

    /**
     * webId : identifiants pour se connecter au serveur Web courant. Pas de
     * valeur par défaut, ils doivent être fournis dans le fichier
     * CheckProviderContact.prop.
     */
    private Identifiants webId;

    /**
     * dbId : identifiants pour se connecter à la base de données courante. Pas
     * de valeur par défaut, ils doivent être fournis dans le fichier
     * CheckProviderContact.prop.
     */
    private Identifiants dbId;

    /**
     * company : identifiant unique du cllient dans la base de données métier.
     * Paramètre obligatoire.
     */
    private int company = 0;

    /**
     * debugMode : fonctionnement du programme en mode debug (true/false).
     * Valeur par défaut : false.
     */
    private static boolean debugMode = false;

    /**
     * testMode : fonctionnement du programme en mode test (true/false). Valeur
     * par défaut : false.
     */
    private static boolean testMode = false;

    /**
     * Constructeur principal de la classe checkProviderContact
     *
     * @param args arguments de la ligne de commande.
     * @throws checkprovidercontact.GetArgsException en cas d'erreur avec les
     * paramètres en ligne de commande
     */
    public CheckProviderContact(String[] args) throws GetArgsException, IOException, WebServerException, DBServerException, ClassNotFoundException, SQLException, Exception {

        GetArgs getArgs;
        ApplicationProperties applicationProperties;
        WebServer webServer;
        DBServer dbServer;
        DBManager dBManager;
        HttpsClient httpsClient;
        Connection connection;

        System.out.println("Création d'une instance de CheckProviderContact ...");

        System.out.println("Analyse des arguments de la ligne de commande ...");
        getArgs = new GetArgs(args);
        setWebServerType(getArgs.getWebServerType());
        setDbServerType(getArgs.getDbServerType());
        setCompany(getArgs.getCompany());
        setDebugMode(getArgs.getDebugMode());
        setTestMode(getArgs.getTestMode());

        System.out.println("Lecture des paramètres d'exécution ...");
        applicationProperties = new ApplicationProperties("checkProviderContact.prop");

        System.out.println("Lecture des paramètres du serveur Web ...");
        webServer = new WebServer(getWebServerType(), applicationProperties);
        if (debugMode) {
            System.out.println(webServer);
        }
        setWebId(applicationProperties);
        if (debugMode) {
            System.out.println(getWebId());
        }

        System.out.println("Lecture des paramètres du serveur de base de données ...");
        dbServer = new DBServer(getDbServerType(), applicationProperties);
        if (debugMode) {
            System.out.println(dbServer);
        }
        setDbId(applicationProperties);
        if (debugMode) {
            System.out.println(getDbId());
        }

        System.out.println("Ouverture de la connexion au site Web : " + webServer.getName());
        httpsClient = new HttpsClient(webServer.getIpAddress(), getWebId(), debugMode, testMode);

        System.out.println("Authentification en cours ...");
        httpsClient.sendPost(HttpsClient.REST_API_PATH + HttpsClient.LOGIN_CMDE);

        System.out.println("Ouverture de la connexion au serveur de base de données : " + dbServer.getName());
        dBManager = new DBManager(dbServer);

        System.out.println("Connexion à la base de données : " + dbServer.getDbName());
        connection = dBManager.getConnection();

        // Compare la base de données locale à celle de l'extranet
        compareLocaleDb_2_WebDb(connection, getCompany(), httpsClient);

        // Compare la base de données de l'extranet à celle en local
        compareWebDb_2_LocaleDb(connection, getCompany());
    }

    private void compareLocaleDb_2_WebDb(Connection connection, int company, HttpsClient httpsClient) {
        Furgent emergencyService;
        FurgentDAO furgentDAO;
        Ftoubib providerContactFromDB;
        FtoubibDAO ftoubibDAO;
        int i;
        int j;
        String aggregateUid;
        String command;
        ObjectMapper objectMapper;
        String response;
        ProviderContact providerContactFromWeb;
        FileOutputStream out;
        XSSFWorkbook classeur;
        XSSFSheet feuille;
        XSSFRow titre;
        XSSFCell cell;
        XSSFRow ligne;
        XSSFCellStyle cellStyle;
        XSSFCellStyle titleStyle;
        XSSFCellStyle cellStyle2;

        System.out.println("Comparaison de la base de données locale à celle de l'Extranet ...");

//      Création d'un classeur Excel
        classeur = new XSSFWorkbook();

        // Style de cellule avec bordure noire
        cellStyle = classeur.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());

        // Style pour le titre
        titleStyle = (XSSFCellStyle) cellStyle.clone();
        titleStyle.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        titleStyle.setFillPattern(FillPatternType.LESS_DOTS);
//        titleStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());

        // Style pour les cellules à renvoi à la ligne automatique
        cellStyle2 = (XSSFCellStyle) cellStyle.clone();
        cellStyle2.setAlignment(HorizontalAlignment.JUSTIFY);
        cellStyle2.setVerticalAlignment(VerticalAlignment.JUSTIFY);

        try {
            command = HttpsClient.REST_API_PATH + HttpsClient.PROVIDER_CONTACTS_CMDE;
            if (debugMode) {
                System.out.println("  Commande pour récupérer les providerContacts : " + command);
            }
            objectMapper = new ObjectMapper();

            furgentDAO = new FurgentDAO(connection);
            if (company > 0) {
                furgentDAO.filterById(company);
            }
            furgentDAO.orderBy("unum");
            System.out.println("  SelectStatement=" + furgentDAO.getSelectStatement());
            furgentDAO.setSelectPreparedStatement();
            i = 0;
            while ((emergencyService = furgentDAO.select()) != null) {
                i++;
                System.out.println("Client(" + i + ")=" + emergencyService);
                aggregateUid = Md5.encode("u:" + emergencyService.getUnum());
                System.out.println("  aggregateUid:" + aggregateUid);

                feuille = classeur.createSheet(emergencyService.getUname() + "_" + emergencyService.getUnum());
                
                // Ligne de titre
                titre = feuille.createRow(0);
                cell = titre.createCell((short) 0);
                cell.setCellStyle(titleStyle);
                cell.setCellValue("Nom");
                cell = titre.createCell((short) 1);
                cell.setCellStyle(titleStyle);
                cell.setCellValue("Prénom");
                cell = titre.createCell((short) 2);
                cell.setCellStyle(titleStyle);
                cell.setCellValue("Id");
                cell = titre.createCell((short) 3);
                cell.setCellStyle(titleStyle);
                cell.setCellValue("UUID");
                cell = titre.createCell((short) 4);
                cell.setCellStyle(titleStyle);
                cell.setCellValue("Etat");

                ftoubibDAO = new FtoubibDAO(connection);
                ftoubibDAO.filterByGid(company);
                ftoubibDAO.orderBy("tlname, tfname");
                ftoubibDAO.setSelectPreparedStatement();
                j = 0;
                while ((providerContactFromDB = ftoubibDAO.select()) != null) {
                    j++;

                    ligne = feuille.createRow(j);

                    cell = ligne.createCell(0);
                    cell.setCellValue(providerContactFromDB.getTlname());
                    cell.setCellStyle(cellStyle);

                    cell = ligne.createCell(1);
                    cell.setCellValue(providerContactFromDB.getTfname());
                    cell.setCellStyle(cellStyle);

                    cell = ligne.createCell(2);
                    cell.setCellValue(providerContactFromDB.getTnum());
                    cell.setCellStyle(cellStyle);

                    cell = ligne.createCell(3);
                    cell.setCellValue(providerContactFromDB.getTUuid());
                    cell.setCellStyle(cellStyle);

                    System.out.println("  proviserContact(" + j + ")="
                            + providerContactFromDB.getTlname()
                            + " " + providerContactFromDB.getTfname()
                            + ", id=" + providerContactFromDB.getTnum()
                            + ", uid=" + providerContactFromDB.getTUuid());

                    cell = ligne.createCell(4);
                    cell.setCellStyle(cellStyle);
                    try {
                        httpsClient.sendGet(command + "/" + providerContactFromDB.getTUuid());
                        if (httpsClient.getResponseCode() == 200) {
                            response = httpsClient.getResponse();
//                            System.out.println("  Réponse=" + response);
                            providerContactFromWeb = objectMapper.readValue(response, ProviderContact.class);
                            if (providerContactFromWeb != null) {
                                System.out.println("  Found providerContact:" + providerContactFromWeb);
                                cell.setCellValue("Trouvé");
                            } else {
                                System.out.println("  WARNING : providerContact not found #1");
                                cell.setCellValue("Non trouvé #1");
                            }
                        } else {
                            System.out.println("  WARNING : providerContact not found #2");
                            cell.setCellValue("Non trouvé #2");

                        }
                    } catch (Exception ex) {
                        Logger.getLogger(HttpsClient.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
                ftoubibDAO.closeSelectPreparedStatement();
//                ftoubibDAO.close();
                
                // Ajustement automatique de la largeur des 5 premières colonnes
                for (int k = 0; k < 5; k++) {
                    feuille.autoSizeColumn(k);
                }

                    // Largeur des deux dernières colonnes fixées à 50 = 12 800 / 256
        //            feuille.setColumnWidth((int)6, (int)12800);
        //            feuille.setColumnWidth((int)7, (int)12800);
                // Format A4 en sortie
                feuille.getPrintSetup().setPaperSize(PaperSize.A4_PAPER);

                // Orientation paysage
                feuille.getPrintSetup().setLandscape(true);

                // Ajustement à une page en largeur
                feuille.setFitToPage(true);
                feuille.getPrintSetup().setFitWidth((short) 1);
                feuille.getPrintSetup().setFitHeight((short) 0);

                // En-tête et pied de page
                Header header = feuille.getHeader();
                header.setLeft("Liste des intervenants pour " + emergencyService.getUname());
                header.setRight("&F");

                Footer footer = feuille.getFooter();
                footer.setLeft("Documentation confidentielle Anstel");
                footer.setCenter("Page &P / &N");
                footer.setRight("&D");

                // Ligne à répéter en haut de page
                feuille.setRepeatingRows(CellRangeAddress.valueOf("1:1"));
            }
            furgentDAO.closeSelectPreparedStatement();
//            furgentDAO.close();
        } catch (ClassNotFoundException exception) {
            Logger.getLogger(CheckProviderContact.class.getName()).log(Level.SEVERE, null, exception);
        } catch (SQLException exception) {
            Logger.getLogger(CheckProviderContact.class.getName()).log(Level.SEVERE, null, exception);
        }

        // Enregistrement du classeur dans un fichier
        try {
//            out = new FileOutputStream(new File(path + "\\" + filename));
            out = new FileOutputStream(new File("checkProviderContact.xlsx"));
            classeur.write(out);
            out.close();
//            System.out.println("Fichier Excel " + filename + " créé dans " + path);
            System.out.println("Fichier Excel checkProviderContact.xlsx créé");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CheckProviderContact.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CheckProviderContact.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void compareWebDb_2_LocaleDb(Connection connection, int company) {
        System.out.println("Comparaison de la base de données de l'extranet à celle en local ...");

    }

    /**
     * @param webServerType définit le serveur Web
     */
    private void setWebServerType(String webServerType) {
        this.webServerType = webServerType;
    }

    /**
     * @param dbServerType définit le serveur de base de données
     */
    private void setDbServerType(String dbServerType) {
        this.dbServerType = dbServerType;
    }

    /**
     * @return webServerType le serveur web
     */
    private String getWebServerType() {
        return (webServerType);
    }

    /**
     * @return dbServerType le serveur de base de données
     */
    private String getDbServerType() {
        return (dbServerType);
    }

    /**
     * @param company : définit l'identifiant unique du client.
     */
    public void setCompany(int company) {
        this.company = company;
    }

    /**
     * @param debugMode : fonctionnement du programme en mode debug
     * (true/false).
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * @param testMode : fonctionnement du programme en mode test (true/false).
     */
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /**
     * @return company : identifiant unique du client.
     */
    public int getCompany() {
        return (company);
    }

    /**
     * @return les identifiants pour accéder au serveur Web
     */
    public Identifiants getWebId() {
        return webId;
    }

    /**
     * @param webId définit les identifiants pour accéder au serveur Web
     */
    public void setWebId(Identifiants webId) {
        this.webId = webId;
    }

    /**
     * @param applicationProperties définit les identifiants pour accéder au
     * serveur Web
     * @throws WebServerException en cas d'erreur sur la lecteur des
     * identifiants
     */
    public void setWebId(ApplicationProperties applicationProperties) throws WebServerException {
        String value;
        Identifiants identifiants = new Identifiants();

        value = applicationProperties.getProperty(getWebServerType() + ".webserver.login");
        if (value != null) {
            identifiants.setLogin(value);
        } else {
            throw new WebServerException("Nom utilisateur pour l'accès Web non défini");
        }

        value = applicationProperties.getProperty(getWebServerType() + ".webserver.passwd");
        if (value != null) {
            identifiants.setPassword(value);
        } else {
            throw new WebServerException("Mot de passe pour l'accès Web non défini");
        }
        setWebId(identifiants);
    }

    /**
     * @return les identifiants pour accéder à la base de données
     */
    public Identifiants getDbId() {
        return dbId;
    }

    /**
     * @param dbId définit les identifiants pour accéder à la base de données
     */
    public void setDbId(Identifiants dbId) {
        this.dbId = dbId;
    }

    /**
     * @param applicationProperties définit les identifiants pour accéder au
     * serveur Web
     * @throws WebServerException en cas d'erreur sur la lecteur des
     * identifiants
     */
    public void setDbId(ApplicationProperties applicationProperties) throws WebServerException {
        String value;
        Identifiants identifiants = new Identifiants();

        value = applicationProperties.getProperty(getDbServerType() + ".dbserver.login");
        if (value != null) {
            identifiants.setLogin(value);
        } else {
            throw new WebServerException("Nom utilisateur pour l'accès base de données non défini");
        }

        value = applicationProperties.getProperty(getDbServerType() + ".dbserver.passwd");
        if (value != null) {
            identifiants.setPassword(value);
        } else {
            throw new WebServerException("Mot de passe pour l'accès base de données non défini");
        }
        setDbId(identifiants);
    }

    /**
     * @return debugMode : retourne le mode de fonctionnement debug.
     */
    public boolean getDebugMode() {
        return (debugMode);
    }

    /**
     * @return testMode : retourne le mode de fonctionnement test.
     */
    public boolean getTestMode() {
        return (testMode);
    }

    /**
     * Retourne le contenu de CheckProviderContact
     *
     * @return retourne le contenu de CheckProviderContact
     */
    @Override
    public String toString() {
        return "CheckProviderContact:{webServer=" + getWebServerType()
                + ", dbServer=" + getDbServerType()
                + ", company=" + getCompany()
                + "}";
    }

    /**
     * @param args arguments de la ligne de commande.
     */
    public static void main(String[] args) {
        CheckProviderContact checkProviderContact;

        System.out.println("Lancement de CheckProviderContact ...");

        try {
            checkProviderContact = new CheckProviderContact(args);
            if (debugMode) {
                System.out.println(checkProviderContact);
            }
        } catch (Exception exception) {
            System.out.println("Problème lors de l'instanciation de CheckProviderContact");
            exception.printStackTrace();
        }

        System.out.println("Fin de CheckProviderContact");

    }

}
