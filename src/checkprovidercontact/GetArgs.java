package checkprovidercontact;

import java.util.Date;

/**
 * Cette classe sert à vérifier et à récupérer les arguments passés en ligne de
 * commande au programme checkProviderContact.
 *
 * @author Thierry Baribaud
 * @version 0.02
 */
public class GetArgs {

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
     * company : identifiant unique du cllient dans la base de données métier.
     * Paramètre obligatoire.
     */
    private int company = 0;

    /**
     * fixAnomaly : active ou non la correction des anomalies (true/false).
     * Valeur par défaut : false.
     */
    private boolean fixAnomaly = false;

    /**
     * debugMode : fonctionnement du programme en mode debug (true/false).
     * Valeur par défaut : false.
     */
    private boolean debugMode = false;

    /**
     * testMode : fonctionnement du programme en mode test (true/false). Valeur
     * par défaut : false.
     */
    private boolean testMode = false;

    /**
     * @return webServerType : retourne la valeur pour le serveur source.
     */
    public String getWebServerType() {
        return (webServerType);
    }

    /**
     * @return company : identifiant unique du client.
     */
    public int getCompany() {
        return (company);
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
     * @return fixAnomaly : retourne si les corrections sont activées ou non.
     */
    public boolean getFixAnomaly() {
        return (fixAnomaly);
    }

    /**
     * @param webServerType : définit le serveur web à la source.
     */
    public void setWebServerType(String webServerType) {
        this.webServerType = webServerType;
    }

    /**
     * @param company : définit l'identifiant unique du client.
     */
    public void setCompany(int company) {
        this.company = company;
    }

    /**
     * @param fixAnomaly : définit le mode de correction d'anomalie
     * (true/false).
     */
    public void setFixAnomaly(boolean fixAnomaly) {
        this.fixAnomaly = fixAnomaly;
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
     * @param args arguments de la ligne de commande.
     * @throws GetArgsException en cas d'erreur sur les paramètres
     */
    public GetArgs(String args[]) throws GetArgsException {

        int i;
        int n;
        int ip1;
        Date date;
        int company;

        n = args.length;

//        System.out.println("nargs=" + n);
//    for(i=0; i<n; i++) System.out.println("args["+i+"]="+Args[i]);
        i = 0;
        while (i < n) {
//            System.out.println("args[" + i + "]=" + Args[i]);
            ip1 = i + 1;
            if (args[i].equals("-webserver")) {
                if (ip1 < n) {
                    if (args[ip1].equals("pre-prod") || args[ip1].equals("prod")) {
                        setWebServerType(args[ip1]);
                    } else {
                        throw new GetArgsException("Mauvais serveur web : " + args[ip1]);
                    }
                    i = ip1;
                } else {
                    throw new GetArgsException("Serveur Web non défini");
                }
            } else if (args[i].equals("-dbserver")) {
                if (ip1 < n) {
                    if (args[ip1].equals("pre-prod") || args[ip1].equals("prod")) {
                        setDbServerType(args[ip1]);
                    } else {
                        throw new GetArgsException("Mauvaise base de données : " + args[ip1]);
                    }
                    i = ip1;
                } else {
                    throw new GetArgsException("Base de données non définie");
                }
            } else if (args[i].equals("-company")) {
                if (ip1 < n) {
                    try {
                        setCompany(Integer.parseInt(args[ip1]));
                        i = ip1;
                    } catch (Exception MyException) {
                        throw new GetArgsException("La référence client doit être numérique : " + args[ip1]);
                    }
                } else {
                    throw new GetArgsException("Référence client non définie");
                }

            } else if (args[i].equals("-fix")) {
                setFixAnomaly(true);
            } else if (args[i].equals("-d")) {
                setDebugMode(true);
            } else if (args[i].equals("-t")) {
                setTestMode(true);
            } else {
                throw new GetArgsException("Mauvais argument : " + args[i]);
            }
            i++;
        }

        if ((company = getCompany()) == 0) {
            usage();
            throw new GetArgsException("Client non définit");
        }
    }

    /**
     * Affiche le mode d'utilisation du programme.
     */
    public static void usage() {
        System.out.println("Usage : java checkProviderContact [-webserver prod|pre-prod]"
                + " [-dbserver prod|pre-prod]"
                + " -companies id_company [-fix]"
                + " [-d] [-t]");
    }

    /**
     * @return le serveur de base de données de destination
     */
    public String getDbServerType() {
        return dbServerType;
    }

    /**
     * @param dbServerType définit le serveur de base de données de destination
     */
    public void setDbServerType(String dbServerType) {
        this.dbServerType = dbServerType;
    }

    /**
     * Affiche le contenu de GetArgs.
     *
     * @return retourne le contenu de GetArgs.
     */
    @Override
    public String toString() {
        return "GetArg: {"
                + "webServerType:" + getWebServerType()
                + ", dbServerType:" + getDbServerType()
                + ", fixAnomaly:" + getFixAnomaly()
                + ", debugMode:" + getDebugMode()
                + ", testMode:" + getTestMode()
                + "}";
    }
}
