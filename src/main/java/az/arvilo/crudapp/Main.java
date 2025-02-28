package az.arvilo.crudapp;

import az.arvilo.crudapp.service.Service;

public class Main {

    public static void main(String[] args) {
        Service service = new Service();
        try (ConsoleApp consoleApp = new ConsoleApp(service)) {
            consoleApp.run();
        }
    }
}
