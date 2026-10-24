/** Exemple de démonstration : contient une fausse clé pour montrer le masquage automatique. */
export const SAMPLE_SOURCE = `public class InvoiceCalculator {

    private static final String API_KEY = "sk-demo-1234567890abcdefghij";

    public double computeTotal(List<Double> amounts, double vatRate) {
        double net = amounts.stream().mapToDouble(Double::doubleValue).sum();
        return net * (1 + vatRate);
    }

    public boolean isEligibleForDiscount(int ordersCount, boolean premium) {
        return premium || ordersCount > 10;
    }

    public static String formatReference(String prefix, int number) {
        return prefix + "-" + String.format("%05d", number);
    }
}
`;
