import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean balancedPolicy = false;
        while (true) {
            try {
                System.out.print("Do you want Balanced Policy to be true? (true/false): ");
                balancedPolicy = scanner.nextBoolean();
                break;
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter 'true' or 'false'.");
                scanner.nextLine();
            }
        }
        System.out.println("The value of Balanced Policy is: " + balancedPolicy);
        scanner.close();

        //inicio hilos

    }
}

/*
    *   T0: requiere 1 cliente afuera del edificio, 1 asistente, la puerta
        cliente cambia estado: por ingresar a ingresado
    *   T1: requiere cliente ingresado, devuelve puerta
        cliente ingresado pasa a sala de espera 1
    *   T2: requiere gestor 1, devuelve asistente, requiere cliente en sala de espera 1
        cliente cambio de estado: de ingresado a con reserva
    *   T5: requiere cliente con reserva, devuelve gestor 1
        cliente con reserva pasa a sala de espera 2
    *   T3: requiere gestor 2, devuelve asistente, requiere cliente en sala de espera 1
        cliente cambio de estado: de ingresado a con reserva
    *   T4: requiere cliente con reserva, devuelve gestor 2
    *   cliente con reserva pasa a sala de espera 2
    *   T6: requiere cliente con reserva en sala de espera 2, requiere agente
        cliente cambio de estado: con reseva a confirmado
    *   T9: requiere cliente confirmado
        cambio de estado de cliente confirmado a pagado
    *   T10: requiere cliente pagado, devuelve agente
        cliente pagado pasa a sala de salida
    *   T7: requiere cliente con reserva en sala de espera 2, requiere agente
        cliente cambio de estado: con reseva a cancelado
    *   T8: requiere cliente cancelado, devuelve agente
        cliente cancelado pasa a sala de salida
    *   T11: requiere cliente en sala de salida
        crea nuevo cliente afuera del edificio

* */