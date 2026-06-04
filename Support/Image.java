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

    // =========================================================================
    // CONSTRUCTEURS
    // =========================================================================
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

    // =========================================================================
    // DATA AUGMENTATION ET TRAITEMENT D'IMAGE (Filtres)
    // =========================================================================

    public Image genererMiroir() {
        int[] donneesMiroir = new int[this.donnees.length];
        boolean gris = estEnNiveauxDeGris();

        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {
                int jMiroir = largeur - 1 - j;

                if (gris) {
                    int indexOrigine = i * largeur + j;
                    int indexMiroir = i * largeur + jMiroir;
                    donneesMiroir[indexMiroir] = this.donnees[indexOrigine];
                } else {
                    int indexOrigine = 3 * (i * largeur + j);
                    int indexMiroir = 3 * (i * largeur + jMiroir);
                    donneesMiroir[indexMiroir + 0] = this.donnees[indexOrigine + 0];
                    donneesMiroir[indexMiroir + 1] = this.donnees[indexOrigine + 1];
                    donneesMiroir[indexMiroir + 2] = this.donnees[indexOrigine + 2];
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

    public Image genererDecalage(int decalageX, int decalageY) {
        int[] donneesDecalees = new int[this.donnees.length];

        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {
                int nouveauX = j + decalageX;
                int nouveauY = i + decalageY;

                if (nouveauX >= 0 && nouveauX < largeur && nouveauY >= 0 && nouveauY < hauteur) {
                    int indexOrigine = i * largeur + j;
                    int indexDecale = nouveauY * largeur + nouveauX;
                    donneesDecalees[indexDecale] = this.donnees[indexOrigine];
                }
            }
        }
        return new Image(this.label, this.largeur, this.hauteur, donneesDecalees);
    }

    public Image genererMasquage(int largeurMasque, int hauteurMasque) {
        int[] donneesMasquees = new int[this.donnees.length];
        System.arraycopy(this.donnees, 0, donneesMasquees, 0, this.donnees.length);
        java.util.Random rand = new java.util.Random(42);

        int xMasque = rand.nextInt(largeur - largeurMasque);
        int yMasque = rand.nextInt(hauteur - hauteurMasque);

        for (int i = yMasque; i < yMasque + hauteurMasque; i++) {
            for (int j = xMasque; j < xMasque + largeurMasque; j++) {
                int index = i * largeur + j;
                donneesMasquees[index] = 0;
            }
        }
        return new Image(this.label, this.largeur, this.hauteur, donneesMasquees);
    }

    public Image appliquerEgalisation() {
        int[] donneesEgalisees = new int[this.donnees.length];
        int[] histogramme = new int[256];

        for (int valeur : this.donnees) {
            histogramme[valeur]++;
        }

        int[] histCumule = new int[256];
        histCumule[0] = histogramme[0];
        for (int i = 1; i < 256; i++) {
            histCumule[i] = histCumule[i - 1] + histogramme[i];
        }

        float totalPixels = this.donnees.length;
        for (int i = 0; i < this.donnees.length; i++) {
            int valeurOrigine = this.donnees[i];
            int nouvelleValeur = Math.round((histCumule[valeurOrigine] * 255.0f) / totalPixels);
            donneesEgalisees[i] = Math.max(0, Math.min(255, nouvelleValeur));
        }

        return new Image(this.label, this.largeur, this.hauteur, donneesEgalisees);
    }

    public Image appliquerFlouGaussien() {
        int[] donneesFloutees = new int[this.donnees.length];

        int[][] noyau = {
                {1, 2, 1},
                {2, 4, 2},
                {1, 2, 1}
        };
        int sommeNoyau = 16;

        for (int i = 1; i < hauteur - 1; i++) {
            for (int j = 1; j < largeur - 1; j++) {
                int sommePonderee = 0;

                for (int ki = -1; ki <= 1; ki++) {
                    for (int kj = -1; kj <= 1; kj++) {
                        int pixelVoisin = this.donnees[(i + ki) * largeur + (j + kj)];
                        sommePonderee += pixelVoisin * noyau[ki + 1][kj + 1];
                    }
                }

                donneesFloutees[i * largeur + j] = sommePonderee / sommeNoyau;
            }
        }

        for (int i = 0; i < this.donnees.length; i++) {
            if (donneesFloutees[i] == 0) {
                donneesFloutees[i] = this.donnees[i];
            }
        }

        return new Image(this.label, this.largeur, this.hauteur, donneesFloutees);
    }

    // =========================================================================
    // EXTRACTION DE CARACTÉRISTIQUES (HOG + TSL + TEXTURE)
    // =========================================================================
    public float[] extraireCaracteristiques() {
        float[] histTeinte = new float[16];
        float[] histSat = new float[8];
        float[] histLum = new float[8];

        int nbZonesX = 5;
        int nbZonesY = 5;
        float[] hogSpatial = new float[nbZonesX * nbZonesY * 9];

        float[] histTexture = new float[8];

        int[] gris = new int[largeur * hauteur];

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

        for (int i = 1; i < hauteur - 1; i++) {
            for (int j = 1; j < largeur - 1; j++) {
                int gx = gris[i * largeur + (j + 1)] - gris[i * largeur + (j - 1)];
                int gy = gris[(i + 1) * largeur + j] - gris[(i - 1) * largeur + j];

                float magnitude = (float) Math.sqrt(gx * gx + gy * gy);

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

        float[] features = new float[265];
        int pos = 0;
        for(float f : histTeinte) features[pos++] = f;
        for(float f : histSat) features[pos++] = f;
        for(float f : histLum) features[pos++] = f;
        for(float f : histTexture) features[pos++] = f;
        for(float f : hogSpatial) features[pos++] = f;

        return features;
    }

    // =========================================================================
    // OUTILS (Sauvegarde et listage)
    // =========================================================================
    public void sauvegarder(String cheminFichier) {
        try {
            BufferedImage imgOut = new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < hauteur; i++) {
                for (int j = 0; j < largeur; j++) {
                    int index = i * largeur + j;
                    int r, g, b;

                    if (estEnNiveauxDeGris()) {
                        r = g = b = donnees[index];
                    } else {
                        r = donnees[3 * index];
                        g = donnees[3 * index + 1];
                        b = donnees[3 * index + 2];
                    }

                    int rgb = (r << 16) | (g << 8) | b;
                    imgOut.setRGB(j, i, rgb);
                }
            }
            ImageIO.write(imgOut, "jpg", new File(cheminFichier));
            System.out.println("-> Image générée avec succès : " + cheminFichier);
        } catch (Exception e) {
            System.out.println("Erreur lors de la création de l'image : " + e.getMessage());
        }
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

    public static void main (String[] args) {
        List<String> cheminsFichiers = listeFichiers("dataset_animaux/");
        if (cheminsFichiers != null) {
            for (String chemin : cheminsFichiers) {
                System.out.println(chemin);
            }
        }

        final String chemin = "dataset_animaux/train/dog/010552.jpg";
        final int labelImage = chemin.indexOf("dog") != -1 ? LabelChien : LabelInconnu;
        Image im1 = new Image(chemin, labelImage, false);
        Image im2 = new Image(chemin, labelImage, true);
        im1.afficheMetadonnees();
        im2.afficheMetadonnees();
    }
}