import java.util.*;
import neurone.*;

public class MainProjet {

    // Chemin vers ton dataset — à adapter si besoin

    static final String CHEMIN_TRAIN = "../dataset_groupe_18/train/";

    static final String CHEMIN_TEST  = "../dataset_groupe_18/test/";

    static final float  MSE_LIMITE   = 0.1f;

    // Convertit et normalise une image en tableau de float entre 0 et 1

    static float[] normalise(Image img) {

        int[] raw = img.donnees();

        float[] result = new float[raw.length];

        for (int i = 0; i < raw.length; i++)

            result[i] = raw[i] / 255.0f;

        return result;

    }

    // Détermine le label d'une image selon le nom de son chemin

    static int labelDepuisChemin(String chemin) {

        if (chemin.contains("cat"))  return 1; // 1 = chat (neurone actif)

        if (chemin.contains("dog"))  return 0; // 0 = chien

        if (chemin.contains("wild")) return 0; // 0 = pas chat

        return -1;

    }

    public static void main(String[] args) {

        System.out.println("=== Chargement des images d'entraînement ===");

        List<String> fichiersTrain = Image.listeFichiers(CHEMIN_TRAIN);

        List<float[]> entreesList  = new ArrayList<>();

        List<Float>   labelsList   = new ArrayList<>();

        for (String chemin : fichiersTrain) {

            int label = labelDepuisChemin(chemin);

            if (label == -1) continue;

            Image img = new Image(chemin, label, true); // true = niveaux de gris

            entreesList.add(normalise(img));

            labelsList.add((float) label);

        }

        System.out.println("Images chargées : " + entreesList.size());

        // Mélange des données

        List<Integer> indices = new ArrayList<>();

        for (int i = 0; i < entreesList.size(); i++) indices.add(i);

        Collections.shuffle(indices);

        int taille = entreesList.get(0).length;

        float[][] entrees  = new float[entreesList.size()][taille];

        float[]   resultats = new float[entreesList.size()];

        for (int i = 0; i < indices.size(); i++) {

            entrees[i]  = entreesList.get(indices.get(i));

            resultats[i] = labelsList.get(indices.get(i));

        }

        // Création et entraînement du neurone

        System.out.println("=== Entraînement du neurone ===");

        iNeurone neurone = new NeuroneSigmoide(taille);

        neurone.apprentissage(entrees, resultats, MSE_LIMITE);

        System.out.println("Entraînement terminé !");

        // Sauvegarde du neurone entraîné

        ((Neurone)neurone).sauvegarde("neurone_entraine.txt");

        // Test

        System.out.println("\n=== Test sur les images de test ===");

        List<String> fichiersTest = Image.listeFichiers(CHEMIN_TEST);

        int correct = 0, total = 0;

        for (String chemin : fichiersTest) {

            int labelAttendu = labelDepuisChemin(chemin);

            if (labelAttendu == -1) continue;

            Image img = new Image(chemin, labelAttendu, true);

            float[] entree = normalise(img);

            neurone.metAJour(entree);

            int prediction = neurone.sortie() >= 0.5f ? 1 : 0;

            if (prediction == labelAttendu) correct++;

            total++;

        }

        System.out.printf("\nRésultat : %d/%d correct (%.1f%%)\n",

                correct, total, 100.0 * correct / total);

    }

}
