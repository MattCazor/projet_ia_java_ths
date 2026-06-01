package neurone;

public class NeuroneSigmoide extends Neurone {

    // Constructeur
    public NeuroneSigmoide(final int nbEntrees) {
        super(nbEntrees);
    }

    // Implémentation de la fonction d'activation Sigmoïde
    @Override
    protected float activation(final float valeur) {
        return (float) (1.0 / (1.0 + Math.exp(-valeur)));
    }
}