package neurone;

public class NeuroneReLU extends Neurone {

    // Constructeur : on appelle le constructeur de la classe parente (Neurone)
    public NeuroneReLU(final int nbEntrees) {
        super(nbEntrees);
    }

    // Implémentation de la fonction d'activation ReLU
    @Override
    protected float activation(final float valeur) {
        // Retourne la valeur si elle est positive, sinon 0
        return Math.max(0.0f, valeur);

        // Alternative avec un opérateur ternaire :
        // return valeur > 0 ? valeur : 0.f;
    }
}