import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javax.imageio.*;

public class Image {
    static private int LabelChat = 0;
    static private int LabelChien = 1;
    static private int LabelWild = 2;
    static private int LabelInconnu = 3;
    private int label = -1;
    private int largeur = 0;
    private int hauteur = 0;
    private int[] donnees = null;

    public int label() {return label;}
    public int largeur() {return largeur;}
    public int hauteur() {return hauteur;}
    public int taille() {return donnees.length;}
    public int[] donnees() {return donnees;}

    public boolean estEnNiveauxDeGris() {return taille() == largeur() * hauteur();}

    public void afficheMetadonnees() {
        String type = estEnNiveauxDeGris() ? "grayscale" : " couleurs";
        System.out.printf("Image (%s): label=%d, largeur=%d, hauteur=%d, taille=%d\n",
                type, label(), largeur(), hauteur(), taille());
    }

    public Image(final String cheminImage, int label, boolean niveauxDeGris) {
        try {
            final BufferedImage img = ImageIO.read(new File(cheminImage));
            this.label = label;
            largeur = img.getWidth(null);
            hauteur = img.getHeight(null);
            final int taille = niveauxDeGris ? hauteur*largeur : 3*hauteur*largeur;
            donnees = new int[taille];
            for (int i = 0; i < hauteur; ++i) {
                for (int j = 0; j < largeur; ++j) {
                    final long rgb = img.getRGB(j, i);
                    final int r = (int)((rgb>>16)&255);
                    final int g = (int)((rgb>>8)&255);
                    final int b = (int)((rgb)&255);
                    final int index = i * largeur + j;
                    if (niveauxDeGris) {
                        final float gris = 0.2125f * r + 0.7154f * g + 0.0721f * b;
                        donnees[index] = (int) Math.max(0, Math.min(255, gris));
                    }
                    else {
                        donnees[3*index+0] = r;
                        donnees[3*index+1] = g;
                        donnees[3*index+2] = b;
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.printf("Image non trouvée ou non lisible: %s\n", cheminImage);
        }
    }

    private Image(int label, int largeur, int hauteur, int[] nouvellesDonnees) {
        this.label = label;
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.donnees = nouvellesDonnees;
    }

    public Image genererMiroir() {
        int[] donneesMiroir = new int[this.donnees.length];
        boolean gris = estEnNiveauxDeGris();

        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {
                int jMiroir = largeur - 1 - j;
                if (gris) {
                    donneesMiroir[i * largeur + jMiroir] = this.donnees[i * largeur + j];
                } else {
                    int idxOrig = 3 * (i * largeur + j);
                    int idxMir = 3 * (i * largeur + jMiroir);
                    donneesMiroir[idxMir + 0] = this.donnees[idxOrig + 0];
                    donneesMiroir[idxMir + 1] = this.donnees[idxOrig + 1];
                    donneesMiroir[idxMir + 2] = this.donnees[idxOrig + 2];
                }
            }
        }
        return new Image(this.label, this.largeur, this.hauteur, donneesMiroir);
    }

    public Image ImageBruit(int intensite) {
        int[] donneesBruitees = new int[this.donnees.length];
        java.util.Random rand = new java.util.Random(42);

        for (int i = 0; i < this.donnees.length; i++) {
            int bruit = rand.nextInt(2 * intensite + 1) - intensite;
            int nouvelleValeur = this.donnees[i] + bruit;
            if (nouvelleValeur > 255) nouvelleValeur = 255;
            if (nouvelleValeur < 0) nouvelleValeur = 0;
            donneesBruitees[i] = nouvelleValeur;
        }
        return new Image(this.label, this.largeur, this.hauteur, donneesBruitees);
    }

    public static List<String> listeFichiers(String repertoire) {
        List<String> cheminsFichiers = null;
        try {
            cheminsFichiers = Files.walk(Paths.get(repertoire))
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cheminsFichiers;
    }

    // =========================================================================
    // L'EXTRACTEUR ULTIME : HOG 5x5 + TSL + TEXTURE (265 Caractéristiques)
    // =========================================================================
    public float[] extraireCaracteristiques() {
        // 1. TSL pour le Fond (32 features)
        float[] histTeinte = new float[16];
        float[] histSat = new float[8];
        float[] histLum = new float[8];

        // 2. HOG Haute Résolution 5x5 pour les Formes (5 * 5 * 9 = 225 features)
        int nbZonesX = 5;
        int nbZonesY = 5;
        float[] hogSpatial = new float[nbZonesX * nbZonesY * 9];

        // 3. L'équivalent de la FFT : Histogramme des Textures (8 features)
        float[] histTexture = new float[8];

        int[] gris = new int[largeur * hauteur];

        // --- A. ANALYSE DU FOND (TSL) ---
        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {
                int index = i * largeur + j;
                int r = 0, g = 0, b = 0;

                if (estEnNiveauxDeGris()) {
                    r = g = b = donnees[index];
                    gris[index] = donnees[index];
                } else {
                    r = donnees[3 * index];
                    g = donnees[3 * index + 1];
                    b = donnees[3 * index + 2];
                    gris[index] = (int)(0.2125f * r + 0.7154f * g + 0.0721f * b);
                }

                float fr = r / 255f, fg = g / 255f, fb = b / 255f;
                float max = Math.max(fr, Math.max(fg, fb));
                float min = Math.min(fr, Math.min(fg, fb));
                float l = (max + min) / 2;
                float s = 0, h = 0;

                if (max != min) {
                    float d = max - min;
                    s = l > 0.5f ? d / (2 - max - min) : d / (max + min);
                    if (max == fr) h = (fg - fb) / d + (fg < fb ? 6 : 0);
                    else if (max == fg) h = (fb - fr) / d + 2;
                    else h = (fr - fg) / d + 4;
                    h /= 6;
                }
                histTeinte[Math.min(15, (int)(h * 16))]++;
                histSat[Math.min(7, (int)(s * 8))]++;
                histLum[Math.min(7, (int)(l * 8))]++;
            }
        }

        // --- B. ANALYSE DES FORMES ET TEXTURES (HOG + FFT Approximation) ---
        for (int i = 1; i < hauteur - 1; i++) {
            for (int j = 1; j < largeur - 1; j++) {
                int gx = gris[i * largeur + (j + 1)] - gris[i * largeur + (j - 1)];
                int gy = gris[(i + 1) * largeur + j] - gris[(i - 1) * largeur + j];

                float magnitude = (float) Math.sqrt(gx * gx + gy * gy);

                // Emulation FFT : On classe l'intensité de la texture
                int texBin = Math.min(7, (int)(magnitude / 32));
                histTexture[texBin]++;

                if (magnitude > 20) {
                    float angle = (float) Math.toDegrees(Math.atan2(gy, gx));
                    if (angle < 0) angle += 180;
                    int bin = (int) (angle / 20) % 9;

                    int zoneX = (j * nbZonesX) / largeur;
                    int zoneY = (i * nbZonesY) / hauteur;
                    if(zoneX >= nbZonesX) zoneX = nbZonesX - 1;
                    if(zoneY >= nbZonesY) zoneY = nbZonesY - 1;

                    int indexHog = (zoneY * nbZonesX + zoneX) * 9 + bin;
                    hogSpatial[indexHog] += magnitude;
                }
            }
        }

        // --- C. NORMALISATION STATISTIQUE ---
        int nbPixels = largeur * hauteur;
        for(int i=0; i<16; i++) histTeinte[i] /= nbPixels;
        for(int i=0; i<8; i++) histSat[i] /= nbPixels;
        for(int i=0; i<8; i++) histLum[i] /= nbPixels;
        for(int i=0; i<8; i++) histTexture[i] /= nbPixels;

        float sumHog = 0;
        for(int i=0; i<hogSpatial.length; i++) sumHog += hogSpatial[i] * hogSpatial[i];
        sumHog = (float) Math.sqrt(sumHog);
        if (sumHog > 0) {
            for(int i=0; i<hogSpatial.length; i++) hogSpatial[i] /= sumHog;
        }

        // --- D. VECTEUR FINAL (265 CARACTÉRISTIQUES) ---
        float[] features = new float[265];
        int pos = 0;
        for(float f : histTeinte) features[pos++] = f;
        for(float f : histSat) features[pos++] = f;
        for(float f : histLum) features[pos++] = f;
        for(float f : histTexture) features[pos++] = f;
        for(float f : hogSpatial) features[pos++] = f;

        return features;
    }
}