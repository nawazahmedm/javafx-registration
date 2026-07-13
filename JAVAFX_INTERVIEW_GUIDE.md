# JavaFX Interview Guide — Senior Java Developer

A comprehensive reference covering architecture, components, threading, patterns,
and security. Each question includes the concept, a direct answer, and a code
example drawn from real-world usage.

---

## Table of Contents

1. [Architecture & Core Concepts](#1-architecture--core-concepts)
2. [Application Lifecycle](#2-application-lifecycle)
3. [Scene Graph & Layouts](#3-scene-graph--layouts)
4. [UI Controls & Components](#4-ui-controls--components)
5. [FXML & MVC Pattern](#5-fxml--mvc-pattern)
6. [Properties & Bindings](#6-properties--bindings)
7. [Event Handling](#7-event-handling)
8. [Concurrency & Threading](#8-concurrency--threading)
9. [CSS Styling](#9-css-styling)
10. [Collections & TableView](#10-collections--tableview)
11. [Navigation & Multi-Screen Apps](#11-navigation--multi-screen-apps)
12. [Security in JavaFX](#12-security-in-javafx)
13. [Performance & Best Practices](#13-performance--best-practices)
14. [Testing JavaFX Applications](#14-testing-javafx-applications)
15. [Packaging & Deployment](#15-packaging--deployment)

---

## 1. Architecture & Core Concepts

---

**Q: What is JavaFX and how does it differ from Swing?**

JavaFX is Oracle's modern UI toolkit for Java desktop and rich client applications.
Key differences from Swing:

| Aspect | Swing | JavaFX |
|--------|-------|--------|
| Release | Java 1.2 (1998) | Java 8+ (2014 bundled) |
| Rendering | AWT-based, software | Hardware-accelerated (Prism) |
| Layout | Manual pixel positioning common | CSS + declarative FXML |
| Threading model | EDT (Event Dispatch Thread) | JavaFX Application Thread |
| Styling | Look-and-Feel | CSS stylesheets |
| Animation | Manual timers | Built-in Timeline, Transitions |
| Declarative UI | None | FXML |

---

**Q: Explain the JavaFX architecture layers.**

```
┌─────────────────────────────────────┐
│         JavaFX Public API           │  ← what you write
├─────────────────────────────────────┤
│      Scene Graph (javafx.scene)     │  ← node tree
├─────────────────────────────────────┤
│   Prism (rendering engine)          │  ← DirectX / OpenGL / Software
├─────────────────────────────────────┤
│   Glass Windowing Toolkit           │  ← OS window management
├─────────────────────────────────────┤
│   Media Engine + Web Engine         │  ← optional modules
└─────────────────────────────────────┘
```

- **Prism** — hardware-accelerated rendering pipeline
- **Glass** — platform-native windowing, input events
- **Quantum** — ties Prism and Glass together, manages threading

---

**Q: What is the Scene Graph?**

The scene graph is a hierarchical tree of `Node` objects that represents
everything displayed on screen. Each node can have children, transformations,
effects, and event handlers.

```java
Stage → Scene → Parent (root)
                  ├── VBox
                  │     ├── Label
                  │     └── Button
                  └── HBox
                        └── TextField
```

Key properties of nodes:
- Every node has exactly one parent (except root)
- Layout, visibility, and transforms cascade down the tree
- Nodes are not thread-safe — only modify on the JavaFX Application Thread

---

## 2. Application Lifecycle

---

**Q: Describe the JavaFX application lifecycle.**

```java
public class MyApp extends Application {

    @Override
    public void init() throws Exception {
        // Called on launcher thread BEFORE start()
        // Use for heavy initialisation (DB connections, config loading)
        // Cannot create Stage or Scene here
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Called on JavaFX Application Thread
        // Build and show your first window here
        primaryStage.setScene(new Scene(new Label("Hello")));
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        // Called on JavaFX Application Thread when app is closing
        // Use for cleanup: close DB, save state, shutdown executors
    }

    public static void main(String[] args) {
        launch(args);  // triggers init() → start()
    }
}
```

Lifecycle order:
1. `main()` calls `launch()`
2. JavaFX runtime initialises
3. `init()` — background thread
4. `start()` — JavaFX Application Thread
5. App runs (event loop)
6. `stop()` — JavaFX Application Thread

---

**Q: Why is a separate `main()` method needed alongside `Application.launch()`?**

When packaging as a module or fat-JAR, the JVM's manifest `Main-Class` must
point to a class that does **not** extend `Application` on some runtimes.
A thin launcher class avoids this:

```java
// App.java extends Application — contains start()
// Main.java is the actual entry point in some setups:
public class Main {
    public static void main(String[] args) {
        App.launch(App.class, args);
    }
}
```

In modern Maven + javafx-maven-plugin setups pointing directly to the
`Application` subclass is fine.

---

## 3. Scene Graph & Layouts

---

**Q: What layout panes are available in JavaFX and when do you use each?**

| Pane | Use case |
|------|----------|
| `VBox` | Stack children vertically |
| `HBox` | Stack children horizontally |
| `BorderPane` | 5 regions: top/bottom/left/right/center — typical app shell |
| `GridPane` | Table-style form layout with rows and columns |
| `FlowPane` | Wrapping row of items (like CSS flexbox) |
| `TilePane` | Fixed-size equal tiles |
| `AnchorPane` | Anchor children to edges — like CSS absolute positioning |
| `StackPane` | Layer children on top of each other |
| `SplitPane` | Resizable split view (like IDE panels) |
| `ScrollPane` | Scrollable wrapper for any content |

```java
// Typical app shell with BorderPane
BorderPane root = new BorderPane();
root.setTop(new MenuBar());        // menu
root.setLeft(new NavigationPane());// sidebar
root.setCenter(new ContentArea()); // main content
root.setBottom(new StatusBar());   // footer
```

---

**Q: What is the difference between `Pane`, `Region`, and `Parent`?**

```
Node
 └── Parent          ← can have children, manages layout
       └── Region    ← adds background, border, padding (CSS-styleable)
             └── Pane ← exposes children list publicly, no auto-layout
                   └── VBox, HBox, GridPane, etc. — specific layout strategies
```

- Use `Region` as a base for custom controls
- Use `Pane` when you want to place children manually
- Use concrete layout classes (`VBox`, `GridPane` etc.) for real forms

---

**Q: How does `HBox.setHgrow()` and `VBox.setVgrow()` work?**

These are **static layout constraints** that tell the parent how a child
should grow when extra space is available:

```java
TextField searchField = new TextField();
HBox.setHgrow(searchField, Priority.ALWAYS); // stretches to fill HBox width

TextArea bio = new TextArea();
VBox.setVgrow(bio, Priority.ALWAYS); // stretches to fill VBox height
```

`Priority` values:
- `ALWAYS` — always grows to fill available space
- `SOMETIMES` — grows only if no ALWAYS sibling needs it
- `NEVER` — never grows (default)

---

## 4. UI Controls & Components

---

**Q: List the core JavaFX UI controls and their use cases.**

| Control | Purpose |
|---------|---------|
| `Label` | Display-only text |
| `TextField` | Single-line text input |
| `PasswordField` | Masked text input |
| `TextArea` | Multi-line text input |
| `Button` | Clickable action trigger |
| `ToggleButton` | Stateful on/off button |
| `CheckBox` | Boolean selection (tri-state: selected/unselected/indeterminate) |
| `RadioButton` | Single selection within a `ToggleGroup` |
| `ComboBox` | Dropdown list, editable or not |
| `ChoiceBox` | Simple dropdown, no editing |
| `ListView` | Scrollable list of items |
| `TableView` | Multi-column data grid |
| `TreeView` | Hierarchical data display |
| `DatePicker` | Calendar date selector |
| `Slider` | Numeric range input |
| `ProgressBar` | Task progress indicator |
| `Spinner` | Numeric increment/decrement input |
| `Hyperlink` | Clickable link (like HTML anchor) |
| `Tooltip` | Hover hint on any node |
| `Separator` | Visual divider line |
| `MenuBar` + `Menu` | Application menu system |
| `ToolBar` | Row of action buttons/controls |
| `TabPane` + `Tab` | Tabbed content switcher |
| `TitledPane` | Collapsible labelled section |
| `Accordion` | One-open-at-a-time TitledPane group |
| `SplitPane` | Resizable side-by-side panels |
| `ScrollPane` | Adds scroll bars to any content |
| `Alert` | Modal dialog: INFO/WARNING/ERROR/CONFIRM |
| `FileChooser` | OS native open/save file dialog |

---

**Q: How do RadioButtons work with ToggleGroup?**

`ToggleGroup` enforces mutual exclusion — only one button can be selected:

```java
ToggleGroup gender = new ToggleGroup();

RadioButton male   = new RadioButton("Male");
RadioButton female = new RadioButton("Female");
RadioButton other  = new RadioButton("Other");

male.setToggleGroup(gender);
female.setToggleGroup(gender);
other.setToggleGroup(gender);

// Read selected value
Toggle selected = gender.getSelectedToggle();
if (selected instanceof RadioButton rb) {
    String value = rb.getText(); // "Male", "Female", or "Other"
}

// Listen for changes
gender.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
    if (newVal instanceof RadioButton rb) {
        System.out.println("Selected: " + rb.getText());
    }
});
```

---

**Q: What is the difference between `ComboBox` and `ChoiceBox`?**

| Feature | ComboBox | ChoiceBox |
|---------|----------|-----------|
| Editable | Yes (`setEditable(true)`) | No |
| Custom cell rendering | Yes (`setCellFactory()`) | Limited |
| Large data sets | Better (virtual scroll) | Poor |
| Type-safe | `ComboBox<T>` | `ChoiceBox<T>` |
| Prompt text | Yes | No |

Prefer `ComboBox` for almost all use cases.

---

## 5. FXML & MVC Pattern

---

**Q: What is FXML and why use it?**

FXML is an XML-based language for declaring JavaFX UIs declaratively,
keeping layout separate from Java logic — the same principle as HTML/CSS
in web development.

Benefits:
- Designers can edit FXML without touching Java code
- Clean separation of concerns (MVC)
- Can be loaded at runtime — supports hot-reload in tools like Scene Builder
- Reduces boilerplate compared to constructing the scene graph in code

```xml
<!-- hello.fxml -->
<VBox xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.example.HelloController">
    <Label fx:id="greetingLabel" text="Hello"/>
    <Button text="Click Me" onAction="#onButtonClick"/>
</VBox>
```

```java
// HelloController.java
public class HelloController {
    @FXML private Label greetingLabel;

    @FXML
    private void onButtonClick() {
        greetingLabel.setText("Clicked!");
    }
}
```

---

**Q: How does FXMLLoader work internally?**

```java
FXMLLoader loader = new FXMLLoader(
    getClass().getResource("/view/registration.fxml"));

Parent root = loader.load(); // parses XML, builds node tree

// After load() you can get the controller instance:
RegistrationViewController ctrl = loader.getController();
ctrl.setUser(someUser); // pass data into the controller
```

Internally:
1. Parses the FXML XML
2. Instantiates the `fx:controller` class via reflection
3. Injects `@FXML`-annotated fields by matching `fx:id` attributes
4. Calls `initialize()` if the controller implements `Initializable`
5. Wires `onAction="#method"` to the matching `@FXML private void method()`

---

**Q: Explain the MVC pattern in JavaFX.**

```
MODEL                   CONTROLLER              VIEW
─────────────────       ──────────────────────  ──────────────────
User.java               UserController.java     registration.fxml
DatabaseHelper.java      ├── registerUser()     RegistrationViewController.java
                         ├── updateUser()
                         └── deleteUser()
```

- **Model** — plain Java objects + database layer. No UI imports.
- **View** — FXML files + ViewController classes. No business logic.
- **Controller** — sits between. Validates input, calls model, returns results.

**Rule:** The View never calls `DatabaseHelper` directly. All data flows through the Controller.

---

**Q: What is the role of `initialize()` in a ViewController?**

`initialize()` is called automatically by `FXMLLoader` after all `@FXML`
fields are injected. It is the safe place to:
- Set up `ToggleGroup` for `RadioButton`s
- Populate `ComboBox` items
- Attach listeners to controls
- Set default values

```java
public class MyController implements Initializable {
    @FXML private ComboBox<String> countryCombo;
    @FXML private Slider ratingSlider;
    @FXML private Label  ratingLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        countryCombo.setItems(FXCollections.observableArrayList(
            "USA", "UK", "India"));

        ratingSlider.valueProperty().addListener((obs, o, n) ->
            ratingLabel.setText(n.intValue() + " / 10"));
    }
}
```

---

## 6. Properties & Bindings

---

**Q: What are JavaFX Properties and why are they important?**

JavaFX Properties are observable wrappers around primitive values. They are
the backbone of data binding — when a property changes, all listeners and
bindings are notified automatically.

```java
// Simple property types
StringProperty  name     = new SimpleStringProperty("John");
IntegerProperty age      = new SimpleIntegerProperty(25);
BooleanProperty active   = new SimpleBooleanProperty(true);
DoubleProperty  progress = new SimpleDoubleProperty(0.0);

// Read / write
name.set("Jane");
System.out.println(name.get()); // "Jane"

// Listen for changes
name.addListener((observable, oldValue, newValue) -> {
    System.out.println("Changed from " + oldValue + " to " + newValue);
});
```

In a Model class, expose properties for binding:

```java
public class User {
    private final StringProperty firstName = new SimpleStringProperty();

    public String getFirstName()                   { return firstName.get(); }
    public void   setFirstName(String v)           { firstName.set(v); }
    public StringProperty firstNameProperty()      { return firstName; }
}
```

---

**Q: What is the difference between unidirectional and bidirectional binding?**

```java
Label nameLabel = new Label();
TextField nameField = new TextField();

// Unidirectional — label always shows what the field contains
// Changes in nameField propagate to nameLabel, but NOT vice versa
nameLabel.textProperty().bind(nameField.textProperty());

// Bidirectional — both stay in sync with each other
nameLabel.textProperty().bindBidirectional(nameField.textProperty());

// Unbind when done (important to prevent memory leaks)
nameLabel.textProperty().unbind();
```

---

**Q: How do you create computed/derived bindings?**

```java
TextField firstName = new TextField();
TextField lastName  = new TextField();
Label     fullName  = new Label();

// Fluent API — concatenate two properties
fullName.textProperty().bind(
    firstName.textProperty()
        .concat(" ")
        .concat(lastName.textProperty())
);

// Bindings utility class for complex expressions
IntegerProperty qty   = new SimpleIntegerProperty(3);
DoubleProperty  price = new SimpleDoubleProperty(19.99);
Label           total = new Label();

total.textProperty().bind(
    Bindings.format("Total: $%.2f", qty.multiply(price))
);
```

---

**Q: What is `ObservableList` and why does TableView need it?**

`ObservableList` is a `List` that fires change events when items are
added, removed, or updated. `TableView` listens to these events and
redraws only the affected rows — you never need to call `refresh()`.

```java
ObservableList<User> users = FXCollections.observableArrayList();
tableView.setItems(users);

users.add(new User("John"));    // table updates immediately
users.remove(0);                // table updates immediately
users.setAll(newList);          // table replaces all rows
```

---

## 7. Event Handling

---

**Q: How does event handling work in JavaFX?**

JavaFX uses a three-phase event dispatch system:

```
Stage → Scene → Parent → ... → Target Node   (Capturing phase, top-down)
Stage ← Scene ← Parent ← ... ← Target Node   (Bubbling phase, bottom-up)
```

```java
Button btn = new Button("Click");

// Handler fires during bubbling phase (most common)
btn.setOnAction(event -> System.out.println("Clicked"));

// Handler fires during capture phase (rare, for interception)
btn.addEventFilter(MouseEvent.MOUSE_PRESSED,
    event -> System.out.println("Intercepted before target"));

// Consume stops propagation
btn.setOnMouseClicked(event -> {
    event.consume(); // stops bubbling up to parent
});
```

---

**Q: What is the difference between `setOnAction` and `addEventHandler`?**

```java
Button btn = new Button();

// setOnAction — convenience method, replaces any previous handler
// Only one handler at a time
btn.setOnAction(e -> System.out.println("Handler A"));
btn.setOnAction(e -> System.out.println("Handler B")); // A is gone

// addEventHandler — adds, does not replace, multiple handlers allowed
btn.addEventHandler(ActionEvent.ACTION, e -> System.out.println("A"));
btn.addEventHandler(ActionEvent.ACTION, e -> System.out.println("B"));
// Both A and B fire

// Remove a specific handler
EventHandler<ActionEvent> handler = e -> System.out.println("A");
btn.addEventHandler(ActionEvent.ACTION, handler);
btn.removeEventHandler(ActionEvent.ACTION, handler);
```

---

**Q: How do you handle keyboard input?**

```java
scene.setOnKeyPressed(event -> {
    switch (event.getCode()) {
        case ENTER  -> submitForm();
        case ESCAPE -> cancelForm();
        case F5     -> refreshData();
    }
});

// On a specific field
textField.setOnKeyPressed(event -> {
    if (event.getCode() == KeyCode.TAB) {
        nextField.requestFocus();
        event.consume();
    }
});

// Accelerators (keyboard shortcuts)
KeyCombination ctrlS = new KeyCodeCombination(
    KeyCode.S, KeyCombination.CONTROL_DOWN);
scene.getAccelerators().put(ctrlS, () -> saveAction());
```

---

## 8. Concurrency & Threading

---

**Q: What is the JavaFX Application Thread and why does it matter?**

All UI updates in JavaFX MUST happen on the **JavaFX Application Thread (JAT)**.
Modifying UI nodes from any other thread causes an `IllegalStateException`
or worse — silent corruption and visual glitches.

```java
// WRONG — updating UI from a background thread
new Thread(() -> {
    String data = fetchFromDatabase();
    label.setText(data); // IllegalStateException!
}).start();

// CORRECT — hand off to the JavaFX thread
new Thread(() -> {
    String data = fetchFromDatabase();
    Platform.runLater(() -> label.setText(data)); // safe
}).start();
```

---

**Q: What is `Task` and when do you use it?**

`Task<V>` is JavaFX's equivalent of `SwingWorker` — it runs work on a
background thread and provides hooks to safely update the UI when done.

```java
Task<List<User>> loadTask = new Task<>() {
    @Override
    protected List<User> call() throws Exception {
        // Runs on background thread — safe to do DB/network work
        updateMessage("Loading users...");
        updateProgress(0, 100);

        List<User> users = database.getAllUsers();

        updateProgress(100, 100);
        return users;
    }
};

// These callbacks run on the JAT — safe to update UI
loadTask.setOnSucceeded(e -> {
    List<User> users = loadTask.getValue();
    tableView.setItems(FXCollections.observableArrayList(users));
    statusLabel.setText("Loaded " + users.size() + " users.");
});

loadTask.setOnFailed(e -> {
    statusLabel.setText("Load failed: " + loadTask.getException().getMessage());
});

// Bind progress bar to task progress
progressBar.progressProperty().bind(loadTask.progressProperty());
statusLabel.textProperty().bind(loadTask.messageProperty());

// Run it
new Thread(loadTask).start();
// or: Executors.newSingleThreadExecutor().submit(loadTask);
```

---

**Q: What is the difference between `Task`, `Service`, and `ScheduledService`?**

| Class | Use case |
|-------|---------|
| `Task` | Single one-shot background operation |
| `Service` | Reusable — can be restarted multiple times (e.g. search) |
| `ScheduledService` | Repeats on a schedule (e.g. auto-refresh every 30s) |

```java
// Service — reusable
Service<List<User>> userService = new Service<>() {
    @Override
    protected Task<List<User>> createTask() {
        return new Task<>() {
            @Override
            protected List<User> call() {
                return database.getAllUsers();
            }
        };
    }
};
userService.setOnSucceeded(e ->
    tableView.setItems(FXCollections.observableArrayList(
        userService.getValue())));

userService.start();          // first run
userService.restart();        // subsequent runs

// ScheduledService — repeating
ScheduledService<List<User>> poller = new ScheduledService<>() {
    @Override
    protected Task<List<User>> createTask() {
        return new Task<>() {
            @Override protected List<User> call() {
                return database.getAllUsers();
            }
        };
    }
};
poller.setPeriod(Duration.seconds(30)); // poll every 30 seconds
poller.start();
```

---

**Q: When would you use `Platform.runLater()` vs `Task`?**

| Scenario | Use |
|----------|-----|
| Small UI update from a thread you don't control | `Platform.runLater()` |
| Long-running operation with progress reporting | `Task` |
| Operation that can be cancelled | `Task` (has `cancel()`) |
| Repeated background polling | `ScheduledService` |
| Fire-and-forget UI flash | `Platform.runLater()` |

```java
// Platform.runLater for quick UI updates
someThread.onDataReceived(data ->
    Platform.runLater(() -> label.setText(data)));

// Task for cancellable work
Task<Void> task = new Task<>() {
    @Override protected Void call() throws Exception {
        for (int i = 0; i < 1000; i++) {
            if (isCancelled()) break; // respect cancellation
            process(i);
            updateProgress(i, 1000);
        }
        return null;
    }
};
Button cancelBtn = new Button("Cancel");
cancelBtn.setOnAction(e -> task.cancel());
```

---


## 9. CSS Styling

---

**Q: How does CSS work in JavaFX?**

JavaFX supports a subset of CSS 2.1 with JavaFX-specific extensions.
Stylesheets are applied to a `Scene` or any individual `Node`.

```java
// Apply to entire scene
scene.getStylesheets().add(
    getClass().getResource("/css/style.css").toExternalForm());

// Apply to a single node
button.getStylesheets().add("/css/buttons.css");

// Apply inline style
button.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white;");
```

```css
/* style.css — JavaFX CSS uses -fx- prefix for all properties */
.button {
    -fx-background-color: #1a73e8;
    -fx-text-fill: white;
    -fx-font-size: 13px;
    -fx-padding: 8 20;
    -fx-background-radius: 4;
    -fx-cursor: hand;
}

.button:hover {
    -fx-background-color: #1557b0;
}

.button:pressed {
    -fx-background-color: #0d47a1;
}
```

---

**Q: How do `styleClass` and `id` work in JavaFX CSS?**

```java
// styleClass — same as CSS class selector (.)
// Multiple classes allowed on one node
button.getStyleClass().add("btn-primary");
button.getStyleClass().addAll("btn", "btn-large");
button.getStyleClass().remove("btn-large");

// id — same as CSS id selector (#)
// Only one id per node
button.setId("submitBtn");
```

```css
/* Class selector */
.btn-primary {
    -fx-background-color: #1a73e8;
}

/* ID selector */
#submitBtn {
    -fx-font-weight: bold;
}

/* Pseudo-class (state-based) */
.text-field:focused {
    -fx-border-color: #1a73e8;
}

.table-row-cell:selected {
    -fx-background-color: #e8f0fe;
}
```

---

**Q: How do you dynamically change styles at runtime?**

```java
// Swap entire stylesheet (e.g. dark/light theme toggle)
scene.getStylesheets().clear();
scene.getStylesheets().add("/css/dark-theme.css");

// Toggle a class on/off
label.getStyleClass().removeAll("status-error", "status-success");
label.getStyleClass().add(isSuccess ? "status-success" : "status-error");

// Inline style for one-off dynamic values (e.g. chart bar colour)
bar.setStyle("-fx-background-color: " + hexColour + ";");
```

---

## 10. Collections & TableView

---

**Q: How do you populate and configure a TableView?**

```java
// 1. Define columns — PropertyValueFactory uses reflection
//    to call User.getFirstName(), User.getEmail() etc.
TableColumn<User, String> colName = new TableColumn<>("Name");
colName.setCellValueFactory(new PropertyValueFactory<>("firstName"));

TableColumn<User, String> colEmail = new TableColumn<>("Email");
colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

// 2. Custom value from a lambda (no getter required)
TableColumn<User, String> colFull = new TableColumn<>("Full Name");
colFull.setCellValueFactory(data ->
    new SimpleStringProperty(
        data.getValue().getFirstName() + " " + data.getValue().getLastName()));

// 3. Add columns to table
tableView.getColumns().addAll(colName, colEmail, colFull);

// 4. Set data
ObservableList<User> data = FXCollections.observableArrayList(users);
tableView.setItems(data);
```

---

**Q: How do you add action buttons inside a TableView column?**

This is a common pattern — buttons are created per-row inside a custom
`TableCell`:

```java
TableColumn<User, Void> colActions = new TableColumn<>("Actions");

colActions.setCellFactory(col -> new TableCell<>() {
    private final Button editBtn   = new Button("Edit");
    private final Button deleteBtn = new Button("Delete");
    private final HBox   box       = new HBox(6, editBtn, deleteBtn);

    {
        // Wire handlers — getIndex() gives the row number
        editBtn.setOnAction(e -> {
            User user = getTableView().getItems().get(getIndex());
            openEditDialog(user);
        });
        deleteBtn.setOnAction(e -> {
            User user = getTableView().getItems().get(getIndex());
            confirmDelete(user);
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        // IMPORTANT: return null graphic for empty rows
        setGraphic(empty ? null : box);
    }
});
```

---

**Q: How do you implement live search/filtering on a TableView?**

Use `FilteredList` wrapping your `ObservableList`, then wrap that in a
`SortedList` so column sorting still works:

```java
ObservableList<User> masterData = FXCollections.observableArrayList(users);

// FilteredList — predicate controls what's visible
FilteredList<User> filtered = new FilteredList<>(masterData, u -> true);

searchField.textProperty().addListener((obs, oldVal, newVal) ->
    filtered.setPredicate(user -> {
        if (newVal == null || newVal.isBlank()) return true;
        String lower = newVal.toLowerCase();
        return user.getFirstName().toLowerCase().contains(lower)
            || user.getEmail().toLowerCase().contains(lower);
    }));

// SortedList — keeps column sort working
SortedList<User> sorted = new SortedList<>(filtered);
sorted.comparatorProperty().bind(tableView.comparatorProperty());

tableView.setItems(sorted);
```

---

**Q: What is the difference between `ObservableList`, `FilteredList`, and `SortedList`?**

| Class | Purpose |
|-------|---------|
| `ObservableList` | Master data — fires events on add/remove/update |
| `FilteredList` | View of ObservableList matching a predicate — read-only |
| `SortedList` | Sorted view of another list — read-only |

They chain: `ObservableList → FilteredList → SortedList → TableView`.
Only modify the `ObservableList`; the others update automatically.

---

## 11. Navigation & Multi-Screen Apps

---

**Q: What are the common approaches to multi-screen navigation in JavaFX?**

**Approach 1 — Scene swapping (used in this project)**
Replace the entire scene on the same stage. Simple, low memory.

```java
private void navigateTo(String fxmlPath, String title) throws IOException {
    URL fxml = getClass().getResource(fxmlPath);
    Scene scene = new Scene(new FXMLLoader(fxml).load());
    Stage stage = (Stage) anyNode.getScene().getWindow();
    stage.setTitle(title);
    stage.setScene(scene);
}
```

**Approach 2 — Root node swapping**
Keep the same scene, just replace the root node. Preserves window size.

```java
Parent newRoot = FXMLLoader.load(getClass().getResource("/view/list.fxml"));
stage.getScene().setRoot(newRoot);
```

**Approach 3 — Modal windows**
Open a second `Stage` for dialogs, block input to parent with `Modality`.

```java
Stage dialog = new Stage();
dialog.initModality(Modality.APPLICATION_MODAL);
dialog.initOwner(parentStage);
dialog.setScene(new Scene(FXMLLoader.load(fxml)));
dialog.showAndWait(); // blocks until closed
```

**Approach 4 — StackPane-based router**
Push/pop views onto a `StackPane`. Good for animated transitions.

```java
StackPane router = new StackPane();

void push(Parent view) {
    router.getChildren().add(view);
}
void pop() {
    if (router.getChildren().size() > 1)
        router.getChildren().remove(router.getChildren().size() - 1);
}
```

---

**Q: How do you pass data between screens?**

```java
// After loading the FXML, get the controller and call a setter
FXMLLoader loader = new FXMLLoader(fxml);
Parent root = loader.load();

EditUserViewController ctrl = loader.getController();
ctrl.setUser(selectedUser);   // pass data in

Stage stage = new Stage();
stage.setScene(new Scene(root));
stage.showAndWait();

// Read data back after the dialog closes
User updated = ctrl.getUpdatedUser();
```

---

## 12. Security in JavaFX

---

**Q: How do you securely store passwords in a JavaFX + SQLite app?**

Never store plain-text passwords. Use BCrypt:

```java
// Dependency: org.mindrot:jbcrypt:0.4

// During registration — hash before storing
String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
// Store 'hash' in the database, discard 'plainPassword'

// During login — verify without ever decrypting
boolean valid = BCrypt.checkpw(suppliedPassword, hashFromDatabase);
```

Why BCrypt:
- Built-in salt prevents rainbow table attacks
- Cost factor (12) makes brute-force slow
- Same algorithm produces different hashes for the same input (due to salt)
- One-way: you cannot reverse a BCrypt hash to get the plain password

---

**Q: How do you implement role-based access control in JavaFX?**

Three layers working together:

```java
// 1. SessionManager — who is logged in
public class SessionManager {
    private static String currentRole;

    public static void login(String username, String role) {
        currentRole = role;
    }
    public static boolean isAdmin() {
        return "admin".equalsIgnoreCase(currentRole);
    }
}

// 2. Controller — server-side enforcement
public ValidationResult deleteUser(int id) {
    if (!SessionManager.isAdmin())
        return ValidationResult.fail("Permission denied.");
    return db.deleteUser(id) ? ValidationResult.success("Deleted.")
                             : ValidationResult.fail("Failed.");
}

// 3. View — UI enforcement (disable button, not just hide it)
deleteBtn.setDisable(!SessionManager.isAdmin());
deleteBtn.setTooltip(new Tooltip("Admin only"));
```

Always enforce at the controller layer — UI-only enforcement can be bypassed.

---

**Q: How do you prevent SQL injection in JavaFX apps?**

Always use `PreparedStatement`. Never concatenate user input into SQL strings.

```java
// WRONG — SQL injection vulnerability
String sql = "SELECT * FROM users WHERE email = '" + email + "'";
// Attacker input: ' OR '1'='1  → returns all rows

// CORRECT — parameterised query
String sql = "SELECT * FROM users WHERE email = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, email);  // input is escaped automatically
ResultSet rs = ps.executeQuery();
```

---

**Q: What is an audit log and how do you implement one?**

An audit log is an append-only record of who did what and when.
It is never deleted — only inserted to.

```sql
CREATE TABLE audit_log (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    action       TEXT NOT NULL,        -- INSERT, UPDATE, DELETE, LOGIN
    target_table TEXT NOT NULL,
    target_id    INTEGER,
    description  TEXT,
    performed_by TEXT NOT NULL,        -- logged-in username
    performed_at TEXT NOT NULL         -- ISO timestamp
);
```

```java
// Call after every write operation
db.logAudit("DELETE", "users", user.getId(),
    "Deleted user: " + user.getEmail(),
    SessionManager.getCurrentUsername());
```

---

**Q: How do you implement session management in a desktop app?**

Desktop apps don't have HTTP sessions. Use a static singleton:

```java
public class SessionManager {
    private static String  username;
    private static String  role;
    private static boolean loggedIn;

    public static void login(String u, String r) {
        username = u; role = r; loggedIn = true;
    }

    public static void logout() {
        username = null; role = null; loggedIn = false;
    }

    public static boolean isLoggedIn() { return loggedIn; }
    public static String  getRole()    { return role; }
    public static boolean isAdmin()    { return "admin".equalsIgnoreCase(role); }
}
```

On logout — always call `SessionManager.logout()` and navigate back to the
login screen so the next user cannot access a previous session.

---

## 13. Performance & Best Practices

---

**Q: What are common performance pitfalls in JavaFX?**

**1. Updating UI from a background thread**
Always use `Platform.runLater()` or `Task`.

**2. Loading large data sets on the JAT**
Move DB/network calls to a `Task`. The UI freezes if you block the JAT.

**3. Not using virtual scrolling**
`TableView`, `ListView`, and `TreeView` are virtualised by default — they
only render visible rows. Do not replace them with `VBox` + many `Label`s.

**4. Creating new `Image` objects repeatedly**
Cache images:
```java
// Bad — reloads from disk on every row render
image.setImage(new Image("/icons/edit.png"));

// Good — load once, reuse
private static final Image EDIT_ICON = new Image("/icons/edit.png");
image.setImage(EDIT_ICON);
```

**5. Forgetting to unbind properties**
Bindings hold references. Unbind when a node is removed:
```java
label.textProperty().unbind();
```

---

**Q: What design patterns are commonly used in JavaFX?**

| Pattern | Where used |
|---------|-----------|
| MVC | FXML + ViewController + Model/DB separation |
| Singleton | `DatabaseHelper`, `SessionManager` |
| Observer | JavaFX Properties + Listeners |
| Command | `EventHandler<ActionEvent>` |
| Factory | `CellFactory` for `TableView` / `ListView` |
| Facade | `UserController` hiding `DatabaseHelper` complexity |
| Template Method | `Task.call()` — subclasses define the work |

---

**Q: How do you structure a large JavaFX application?**

```
com.company.app/
├── App.java                    ← entry point only
├── model/                      ← POJOs, no UI imports
├── database/                   ← JDBC / JPA layer
├── security/                   ← auth, session, hashing
├── controller/                 ← business logic, validation
├── view/                       ← FXML controllers only
└── util/                       ← helpers, converters, validators

resources/
├── view/*.fxml                 ← one per screen
├── css/*.css                   ← one global + optional per-screen
└── images/
```

Rules:
- `model/` has zero JavaFX imports
- `controller/` has zero FXML/UI imports
- `view/` never calls `DatabaseHelper` directly
- Each FXML has exactly one controller class

---

## 14. Testing JavaFX Applications

---

**Q: How do you unit test JavaFX controllers?**

The controller business logic (validation, data transformation) can be tested
without launching a UI. Only the `@FXML`-wired fields need the toolkit.

```java
// Test UserController (no UI needed)
class UserControllerTest {

    private UserController controller;

    @BeforeEach
    void setUp() {
        controller = new UserController();
    }

    @Test
    void registerUser_missingEmail_returnsFailure() {
        User user = new User("John", "Doe", "", "555-1234",
            "Male", "USA", "", false, false, "");
        var result = controller.registerUser(user, "Password1", "Password1");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Email"));
    }

    @Test
    void registerUser_passwordMismatch_returnsFailure() {
        User user = new User("John", "Doe", "john@test.com", "555-1234",
            "Male", "USA", "", false, true, "");
        var result = controller.registerUser(user, "Password1", "Password2");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("match"));
    }
}
```

---

**Q: How do you test JavaFX UI with TestFX?**

TestFX is the standard library for JavaFX UI testing:

```xml
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>testfx-junit5</artifactId>
    <version>4.0.18</version>
    <scope>test</scope>
</dependency>
```

```java
@ExtendWith(ApplicationExtension.class)
class LoginViewTest {

    @Start
    void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(
            getClass().getResource("/view/login.fxml"));
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void loginWithValidAdmin_navigatesToUserList(FxRobot robot) {
        robot.clickOn("#usernameField").write("admin");
        robot.clickOn("#passwordField").write("admin123");
        robot.clickOn(".btn-primary");

        // Assert navigation happened
        robot.lookup(".table-view").query(); // should exist
    }

    @Test
    void loginWithWrongPassword_showsError(FxRobot robot) {
        robot.clickOn("#usernameField").write("admin");
        robot.clickOn("#passwordField").write("wrongpassword");
        robot.clickOn(".btn-primary");

        Label status = robot.lookup("#statusLabel").query();
        assertTrue(status.isVisible());
        assertTrue(status.getText().contains("Invalid"));
    }
}
```

---

## 15. Packaging & Deployment

---

**Q: What are the options for distributing a JavaFX application?**

**Option 1 — Fat JAR**
All dependencies bundled. User needs JDK installed.

```xml
<!-- maven-shade-plugin in pom.xml -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals><goal>shade</goal></goals>
            <configuration>
                <mainClass>com.learn.registration.App</mainClass>
            </configuration>
        </execution>
    </executions>
</plugin>
```
```cmd
mvn package
java -jar target/javafx-registration-1.0-SNAPSHOT.jar
```

---

**Option 2 — jlink (custom runtime image)**
Bundles only the JDK modules the app actually uses. Smaller than full JDK.

```cmd
mvn javafx:jlink
target/image/bin/java -m com.learn.registration/com.learn.registration.App
```

---

**Option 3 — jpackage (native installer, recommended)**
Bundles the JRE + app into a platform-native installer. No JDK required on
the end user's machine.

```cmd
jpackage ^
  --input target ^
  --main-jar javafx-registration-1.0.jar ^
  --main-class com.learn.registration.App ^
  --type exe ^
  --name "UserRegistration" ^
  --app-version 1.0 ^
  --win-shortcut ^
  --win-menu
```

Produces `UserRegistration-1.0.exe` — a standard Windows installer.

---

**Q: What is the Java Module System (JPMS) and why does JavaFX require it?**

Since Java 9, the JDK is split into modules (`java.base`, `javafx.controls`,
etc.). JavaFX uses JPMS to control which packages are accessible at runtime.

`module-info.java` is required to:
1. Declare which JavaFX modules your app needs (`requires`)
2. Allow FXML's reflective access to your controller classes (`opens`)
3. Export your packages for other modules to use (`exports`)

```java
module com.learn.registration {
    requires javafx.controls;   // TableView, Button, etc.
    requires javafx.fxml;       // FXMLLoader
    requires java.sql;          // JDBC
    requires org.xerial.sqlitejdbc;
    requires jbcrypt;

    // FXMLLoader uses reflection to inject @FXML fields
    opens com.learn.registration.view       to javafx.fxml;
    opens com.learn.registration.controller to javafx.fxml;
    opens com.learn.registration.model      to javafx.fxml, javafx.base;

    exports com.learn.registration;
}
```

Common mistake: forgetting `opens` causes `InaccessibleObjectException`
at runtime when FXMLLoader tries to inject `@FXML` fields.

---

## Quick Reference — Senior-Level Topics Checklist

Use this before an interview to confirm you can speak to each area:

| Topic | Key points to know |
|-------|--------------------|
| Scene graph | Node tree, JAT-only, cascading transforms |
| FXML + MVC | FXMLLoader, @FXML injection, initialize(), controller lifecycle |
| Properties | Observable wrappers, unidirectional/bidirectional binding, Bindings API |
| ObservableList | FXCollections, FilteredList, SortedList chaining |
| Threading | JAT rule, Platform.runLater, Task, Service, ScheduledService |
| CSS | -fx- prefix, styleClass vs id, pseudo-classes, runtime theme swap |
| TableView | PropertyValueFactory, custom CellFactory, action columns |
| Navigation | Scene swap, modal Stage, data passing via controller setters |
| Security | BCrypt, PreparedStatement, SessionManager, role-gated UI + controller |
| Testing | Unit-test controllers without UI, TestFX for UI tests |
| Deployment | jpackage for native installer, module-info.java requirements |
| Design patterns | MVC, Singleton, Observer, Factory, Facade, Command |

---

*All code examples in this guide are based on the javafx-registration
project in this repository — a working reference you can run and explore.*
