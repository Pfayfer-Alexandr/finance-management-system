import java.io.*;
import java.util.*;

class FinanceUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private Wallet wallet;

    public FinanceUser(String username, String password) {
        this.username = username;
        this.password = password;
        this.wallet = new Wallet();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Wallet getWallet() {
        return wallet;
    }
}

class Wallet implements Serializable {
    private static final long serialVersionUID = 1L;
    private double balance;
    private Map<String, List<Double>> incomes;
    private Map<String, List<Double>> expenses;
    private Map<String, Double> budgets;

    public Wallet() {
        this.balance = 0.0;
        this.incomes = new HashMap<>();
        this.expenses = new HashMap<>();
        this.budgets = new HashMap<>();
    }

    public void addIncome(String category, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма дохода должна быть положительной.");
        }
        incomes.computeIfAbsent(category, k -> new ArrayList<>()).add(amount);
        balance += amount;
    }

    public void addExpense(String category, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Сумма расхода должна быть положительной.");
        }
        expenses.computeIfAbsent(category, k -> new ArrayList<>()).add(amount);
        balance -= amount;

        // Проверка на превышение бюджета
        Double remainingBudget = getRemainingBudget(category);
        if (remainingBudget != null && remainingBudget < 0) {
            System.out.println("Внимание: превышен бюджет для категории '" + category + "'!");
        }

        // Проверка на превышение расходов над доходами
        if (balance < 0) {
            System.out.println("Внимание: расходы превышают доходы!");
        }
    }

    public void setBudget(String category, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Бюджет должен быть положительным.");
        }
        budgets.put(category, amount);
    }

    public double getTotalIncome() {
        return incomes.values().stream()
                .flatMap(List::stream)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    public double getTotalExpense() {
        return expenses.values().stream()
                .flatMap(List::stream)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    public Double getRemainingBudget(String category) {
        if (!budgets.containsKey(category)) {
            return null;
        }
        double totalExpenses = expenses.getOrDefault(category, new ArrayList<>())
                .stream()
                .mapToDouble(Double::doubleValue)
                .sum();
        return budgets.get(category) - totalExpenses;
    }

    public double getTotalIncomeForCategories(List<String> categories) {
        return categories.stream()
                .filter(incomes::containsKey)
                .flatMap(category -> incomes.get(category).stream())
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    public double getTotalExpenseForCategories(List<String> categories) {
        return categories.stream()
                .filter(expenses::containsKey)
                .flatMap(category -> expenses.get(category).stream())
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    public Map<String, Map<String, Double>> getSummary() {
        Map<String, Map<String, Double>> summary = new HashMap<>();

        Map<String, Double> totalIncomeMap = new HashMap<>();
        totalIncomeMap.put("total", getTotalIncome());
        summary.put("total_income", totalIncomeMap);

        Map<String, Double> totalExpenseMap = new HashMap<>();
        totalExpenseMap.put("total", getTotalExpense());
        summary.put("total_expense", totalExpenseMap);

        Map<String, Double> budgetSummary = new HashMap<>();
        for (String category : budgets.keySet()) {
            Double remaining = getRemainingBudget(category);
            budgetSummary.put(category, remaining);
        }
        summary.put("budgets", budgetSummary);

        return summary;
    }

    public void saveSummaryToFile(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            Map<String, Map<String, Double>> summary = getSummary();
            writer.println("Общий доход: " + summary.get("total_income").get("total"));
            writer.println("Общий расход: " + summary.get("total_expense").get("total"));
            writer.println("Бюджеты:");
            for (Map.Entry<String, Double> entry : summary.get("budgets").entrySet()) {
                writer.println(entry.getKey() + ": Остаток бюджета: " + entry.getValue());
            }
            System.out.println("Статистика сохранена в файл: " + filename);
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении файла: " + e.getMessage());
        }
    }
}

class FinanceManager {
    private Map<String, FinanceUser> users;

    public FinanceManager() {
        this.users = new HashMap<>();
    }

    public void registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя пользователя не может быть пустым.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Пароль не может быть пустым.");
        }
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("Пользователь уже существует.");
        }
        users.put(username, new FinanceUser(username, password));
    }

    public FinanceUser loginUser(String username, String password) {
        FinanceUser user = users.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public void saveData(String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public void loadData(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            users = (Map<String, FinanceUser>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            // Если файл не существует, игнорируем ошибку
        }
    }
}

public class Main {
    public static void main(String[] args) {
        FinanceManager manager = new FinanceManager();
        manager.loadData("data.ser");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\n1. Регистрация\n2. Вход\n3. Выход");
            System.out.print("Выберите опцию: ");
            String choice = scanner.nextLine();

            if (choice.equals("1")) {
                System.out.print("Введите имя пользователя: ");
                String username = scanner.nextLine();
                System.out.print("Введите пароль: ");
                String password = scanner.nextLine();
                try {
                    manager.registerUser(username, password);
                    System.out.println("Пользователь успешно зарегистрирован.");
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }

            } else if (choice.equals("2")) {
                System.out.print("Введите имя пользователя: ");
                String username = scanner.nextLine();
                System.out.print("Введите пароль: ");
                String password = scanner.nextLine();
                FinanceUser user = manager.loginUser(username, password);
                if (user != null) {
                    System.out.println("Добро пожаловать, " + username + "!");
                    Wallet wallet = user.getWallet();

                    while (true) {
                        System.out.println("\n1. Добавить доход\n2. Добавить расход\n3. Установить бюджет\n4. Просмотреть статистику\n5. Сохранить статистику в файл\n6. Выйти");
                        System.out.print("Выберите опцию: ");
                        String userChoice = scanner.nextLine();

                        if (userChoice.equals("1")) {
                            System.out.print("Введите категорию дохода: ");
                            String category = scanner.nextLine();
                            System.out.print("Введите сумму дохода: ");
                            try {
                                double amount = Double.parseDouble(scanner.nextLine());
                                wallet.addIncome(category, amount);
                                System.out.println("Доход успешно добавлен.");
                            } catch (NumberFormatException e) {
                                System.out.println("Ошибка: введите корректную сумму.");
                            } catch (IllegalArgumentException e) {
                                System.out.println(e.getMessage());
                            }

                        } else if (userChoice.equals("2")) {
                            System.out.print("Введите категорию расхода: ");
                            String category = scanner.nextLine();
                            System.out.print("Введите сумму расхода: ");
                            try {
                                double amount = Double.parseDouble(scanner.nextLine());
                                wallet.addExpense(category, amount);
                                System.out.println("Расход успешно добавлен.");
                            } catch (NumberFormatException e) {
                                System.out.println("Ошибка: введите корректную сумму.");
                            } catch (IllegalArgumentException e) {
                                System.out.println(e.getMessage());
                            }

                        } else if (userChoice.equals("3")) {
                            System.out.print("Введите категорию бюджета: ");
                            String category = scanner.nextLine();
                            System.out.print("Введите сумму бюджета: ");
                            try {
                                double amount = Double.parseDouble(scanner.nextLine());
                                wallet.setBudget(category, amount);
                                System.out.println("Бюджет успешно установлен.");
                            } catch (NumberFormatException e) {
                                System.out.println("Ошибка: введите корректную сумму.");
                            } catch (IllegalArgumentException e) {
                                System.out.println(e.getMessage());
                            }

                        } else if (userChoice.equals("4")) {
                            Map<String, Map<String, Double>> summary = wallet.getSummary();
                            System.out.println("Общий доход: " + summary.get("total_income").get("total"));
                            System.out.println("Общий расход: " + summary.get("total_expense").get("total"));
                            System.out.println("Бюджеты:");
                            for (Map.Entry<String, Double> entry : summary.get("budgets").entrySet()) {
                                System.out.println(entry.getKey() + ": Остаток бюджета: " + entry.getValue());
                            }

                        } else if (userChoice.equals("5")) {
                            System.out.print("Введите имя файла для сохранения статистики: ");
                            String filename = scanner.nextLine();
                            wallet.saveSummaryToFile(filename);

                        } else if (userChoice.equals("6")) {
                            break;
                        }
                    }
                } else {
                    System.out.println("Неверное имя пользователя или пароль.");
                }

            } else if (choice.equals("3")) {
                manager.saveData("data.ser");
                System.out.println("Выход из приложения...");
                break;
            }
        }
    }
}