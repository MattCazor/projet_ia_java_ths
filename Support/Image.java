import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javax.imageio.*;

public class Image
{
	static private int LabelChat = 0;
	static private int LabelChien = 1;
	static private int LabelWild = 2;
	static private int LabelInconnu = 3;
	private int label = -1;
	private int largeur = 0;
	private int hauteur = 0;
	private int[] donnees = null; // image applatie en concaténant les lignes les unes après les autres

	public int label() {return label;}
	public int largeur() {return largeur;}
	public int hauteur() {return hauteur;}
	public int taille() {return donnees.length;} // nombre de pixels: hauteur*largeur ou 3*hauteur*largeur pour une image RGB
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
					final int r = (int)((rgb>>16)&255);	// Isoler la composante rouge
					final int g = (int)((rgb>>8)&255);	// Isoler la composante verte
					final int b = (int)((rgb)&255);		// Isoler la composante bleue
					final int index = i * largeur + j;
					if (niveauxDeGris) {
						final float gris = 0.2125f * r + 0.7154f * g + 0.0721f * b; // RGB -> niveaux de gris
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

	
    // Constructeur privé permettant de créer une image clonée/modifiée
    
    private Image(int label, int largeur, int hauteur, int[] nouvellesDonnees) {
        this.label = label;
        this.largeur = largeur;
        this.hauteur = hauteur;
        this.donnees = nouvellesDonnees;
    }

    
    // Génère et retourne une nouvelle Image correspondant au miroir horizontal de l'image actuelle
     
    public Image genererMiroir() {
        int[] donneesMiroir = new int[this.donnees.length];
        boolean gris = estEnNiveauxDeGris();

        for (int i = 0; i < hauteur; i++) {
            for (int j = 0; j < largeur; j++) {
                // Colonne cible inversée
                int jMiroir = largeur - 1 - j;

                if (gris) {
                    // En niveaux de gris, 1 pixel = 1 case
                    int indexOrigine = i * largeur + j;
                    int indexMiroir = i * largeur + jMiroir;
                    donneesMiroir[indexMiroir] = this.donnees[indexOrigine];
                } else {
                    // En couleurs RGB, 1 pixel = 3 cases consécutives (Rouge, Green, Bleu)
                    int indexOrigine = 3 * (i * largeur + j);
                    int indexMiroir = 3 * (i * largeur + jMiroir);

                    donneesMiroir[indexMiroir + 0] = this.donnees[indexOrigine + 0]; // R
                    donneesMiroir[indexMiroir + 1] = this.donnees[indexOrigine + 1]; // G
                    donneesMiroir[indexMiroir + 2] = this.donnees[indexOrigine + 2]; // B
                }
            }
        }

        // On return l'image avec les bonnes dimension et le bon label
        return new Image(this.label, this.largeur, this.hauteur, donneesMiroir);
    }

	public static List<String> listeFichiers(String repertoire) {
		List<String> cheminsFichiers = null;
		try {
			// La syntaxe qui suit enchaîne plusieurs méthodes d'affilée
			cheminsFichiers = Files.walk(Paths.get(repertoire))	// Récupère les chemins
				.filter(Files::isRegularFile)					// filtre uniquement les fichiers
				.map(Path::toAbsolutePath)						// convertit le chemin en chemin absolu
				.map(Path::toString)							// convertit le chemin en chaine de caractères
				.collect(Collectors.toList());					// crée une collection à partir de ces chaînes
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cheminsFichiers;
	}

	public static void main (String[] args)
	{
		List<String> cheminsFichiers = listeFichiers("dataset_animaux/");
		for (String chemin : cheminsFichiers) {
			System.out.println(chemin);
		}

		final String chemin = "dataset_animaux/train/dog/010552.jpg";
		final int labelImage = chemin.indexOf("dog") != -1 ? LabelChien : LabelInconnu;
		Image im1 = new Image(chemin, labelImage, false);
		Image im2 = new Image(chemin, labelImage, true);
		im1.afficheMetadonnees();
		im2.afficheMetadonnees();
	}
}
