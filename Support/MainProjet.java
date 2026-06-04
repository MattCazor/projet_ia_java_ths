import neurone.*;
import java.util.*;
import java.io.File;

public class MainProjet {

    static final String CHEMIN_TRAIN = "dataset_groupe_18/train/";
    static final String CHEMIN_TEST  = "dataset_groupe_18/test/";

    static int labelDepuisChemin(String chemin) {
        if (!chemin.endsWith(".jpg") && !chemin.endsWith(".jpeg") && !chemin.endsWith(".png")) return -1;
        if (chemin.contains("cat"))  return 0;
        if (chemin.contains("dog"))  return 1;
        if (chemin.contains("wild")) return 2;
        return -1;
    }

    static void standardiserFeatures(float[] features, float[] moyennes, float[] ecartTypes) {
        for(int i = 0; i < features.length; i++) {
            features[i] = (features[i] - moyennes[i]) / ecartTypes[i];
        }
    }

    public static void main(String[] args) {

        Neurone.fixeCoefApprentissage(0.00001f);

        System.out.println("=== 1. Extraction Intelligente des 265 Caractéristiques ===");
        List<String> fichiersTrain = Image.listeFichiers(CHEMIN_TRAIN);
        List<float[]> entreesList  = new ArrayList<>();
        List<Integer> labelsList   = new ArrayList<>();

        for (String chemin : fichiersTrain) {
            int label = labelDepuisChemin(chemin);
            if (label == -1) continue;

            Image img = new Image(chemin, label, false);
            entreesList.add(img.extraireCaracteristiques());
            labelsList.add(label);

            Image imgMiroir = img.genererMiroir();
            entreesList.add(imgMiroir.extraireCaracteristiques());
            labelsList.add(label);

            Image imgBruitee = img.ImageBruit(10);
            entreesList.add(imgBruitee.extraireCaracteristiques());
            labelsList.add(label);
        }

        int nbFeatures = entreesList.get(0).length;
        float[] moyennesFeatures = new float[nbFeatures];
        float[] ecartTypesFeatures = new float[nbFeatures];

        for(float[] f : entreesList) {
            for(int i=0; i<nbFeatures; i++) moyennesFeatures[i] += f[i];
        }
        for(int i=0; i<nbFeatures; i++) moyennesFeatures[i] /= entreesList.size();

        for(float[] f : entreesList) {
            for(int i=0; i<nbFeatures; i++) {
                ecartTypesFeatures[i] += Math.pow(f[i] - moyennesFeatures[i], 2);
            }
        }
        for(int i=0; i<nbFeatures; i++) {
            ecartTypesFeatures[i] = (float) Math.sqrt(ecartTypesFeatures[i] / entreesList.size());
            if(ecartTypesFeatures[i] == 0) ecartTypesFeatures[i] = 1f;
        }

        for(float[] f : entreesList) {
            standardiserFeatures(f, moyennesFeatures, ecartTypesFeatures);
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < entreesList.size(); i++) indices.add(i);
        Collections.shuffle(indices);

        float[][] entrees = new float[entreesList.size()][nbFeatures];
        float[] ciblesChat    = new float[entreesList.size()];
        float[] ciblesChien   = new float[entreesList.size()];
        float[] ciblesSauvage = new float[entreesList.size()];

        for (int i = 0; i < indices.size(); i++) {
            entrees[i] = entreesList.get(indices.get(i));
            int vraiLabel = labelsList.get(indices.get(i));

            ciblesChat[i]    = (vraiLabel == 0) ? 0.85f : 0.15f;
            ciblesChien[i]   = (vraiLabel == 1) ? 0.85f : 0.15f;
            ciblesSauvage[i] = (vraiLabel == 2) ? 0.85f : 0.15f;
        }

        System.out.println("\n=== 2. Initialisation des 3 Experts ===");

        iNeurone neuroneChat    = new NeuroneSigmoide(nbFeatures);
        iNeurone neuroneChien   = new NeuroneSigmoide(nbFeatures);
        iNeurone neuroneSauvage = new NeuroneSigmoide(nbFeatures);

        String modeleChat = "neurone_chat.txt";
        String modeleChien = "neurone_chien.txt";
        String modeleSauvage = "neurone_sauvage.txt";

        // L'APPRENTISSAGE CONTINU : On charge s'ils existent, mais on ne s'arrête pas là !
        if (new File(modeleChat).exists() && new File(modeleChien).exists() && new File(modeleSauvage).exists()) {
            System.out.println("-> Modèles existants trouvés ! Chargement des connaissances...");
            neuroneChat.chargement(modeleChat);
            neuroneChien.chargement(modeleChien);
            neuroneSauvage.chargement(modeleSauvage);
        } else {
            System.out.println("-> Aucun modèle trouvé. Les neurones partent de zéro.");
        }

        System.out.println("-> Entraînement et Amélioration continue en cours...");

        // L'entraînement est sorti du "else". Il s'exécute à chaque lancement pour affiner les poids.
        neuroneChat.apprentissage(entrees, ciblesChat, 0.025713f);
        neuroneChat.sauvegarde(modeleChat);

        neuroneChien.apprentissage(entrees, ciblesChien, 0.033659f);
        neuroneChien.sauvegarde(modeleChien);

        neuroneSauvage.apprentissage(entrees, ciblesSauvage, 0.04005f);
        neuroneSauvage.sauvegarde(modeleSauvage);

        System.out.println("\n=== 3. Vrais tests de classification (Le Vote) ===");

        List<String> fichiersTest = Image.listeFichiers(CHEMIN_TEST);

        int correctGlobal = 0, totalGlobal = 0;
        int correctChat = 0, totalChat = 0;
        int correctChien = 0, totalChien = 0;
        int correctSauvage = 0, totalSauvage = 0;

        for (String chemin : fichiersTest) {
            int vraiLabel = labelDepuisChemin(chemin);
            if (vraiLabel == -1) continue;

            Image img = new Image(chemin, vraiLabel, false);
            float[] entree = img.extraireCaracteristiques();
            standardiserFeatures(entree, moyennesFeatures, ecartTypesFeatures);

            neuroneChat.metAJour(entree);
            neuroneChien.metAJour(entree);
            neuroneSauvage.metAJour(entree);

            // CALIBRAGE DES SEUILS : Les meilleurs multiplicateurs sont "codés en dur" ici
            float scoreChat    = neuroneChat.sortie()    * 0.92f; // Le chat était trop confiant, on le calme
            float scoreChien   = neuroneChien.sortie()   * 1.04f; // Léger bonus au chien
            float scoreSauvage = neuroneSauvage.sortie() * 1.12f; // Gros bonus au sauvage (classe difficile)

            int predictionFinale = 0;
            float maxScore = scoreChat;

            if (scoreChien > maxScore) {
                predictionFinale = 1;
                maxScore = scoreChien;
            }
            if (scoreSauvage > maxScore) {
                predictionFinale = 2;
                maxScore = scoreSauvage;
            }

            totalGlobal++;
            if (vraiLabel == 0) totalChat++;
            if (vraiLabel == 1) totalChien++;
            if (vraiLabel == 2) totalSauvage++;

            if (predictionFinale == vraiLabel) {
                correctGlobal++;
                if (vraiLabel == 0) correctChat++;
                if (vraiLabel == 1) correctChien++;
                if (vraiLabel == 2) correctSauvage++;
            }
        }

        // AFFICHAGE CLAIR ET PROFESSIONNEL
        System.out.println("--- RÉSULTATS DÉTAILLÉS ---");
        System.out.printf("Précision sur les Chats    : %d/%d (%.1f%%)\n", correctChat, totalChat, (100.0 * correctChat / Math.max(1, totalChat)));
        System.out.printf("Précision sur les Chiens   : %d/%d (%.1f%%)\n", correctChien, totalChien, (100.0 * correctChien / Math.max(1, totalChien)));
        System.out.printf("Précision sur les Sauvages : %d/%d (%.1f%%)\n", correctSauvage, totalSauvage, (100.0 * correctSauvage / Math.max(1, totalSauvage)));
        System.out.println("---------------------------");
        System.out.printf("PRÉCISION GLOBALE (3 classes) : %d/%d correct (%.1f%%)\n", correctGlobal, totalGlobal, 100.0 * correctGlobal / totalGlobal);
    }
}
