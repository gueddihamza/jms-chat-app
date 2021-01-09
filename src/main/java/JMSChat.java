import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

public class JMSChat extends Application {

    private MessageProducer messageProducer;
    private Session session;
    private String codeUser;
    //private Session session2;

    public static void main(String[] args) {
        Application.launch(JMSChat.class);
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("JMS Chat App");
        BorderPane borderPane = new BorderPane();

        HBox hBox = new HBox();
        hBox.setPadding(new Insets(10));
        hBox.setSpacing(10);
        hBox.setBackground(new Background(new BackgroundFill(Color.CADETBLUE , CornerRadii.EMPTY ,Insets.EMPTY)));

        Label labelCode = new Label("Code:");
        labelCode.setPadding(new Insets(5));
        TextField textFieldCode = new TextField("C1");
        textFieldCode.setPromptText("Code");

        Label labelHost = new Label("Host:");
        labelHost.setPadding(new Insets(5));
        TextField textFieldHost = new TextField("localhost");
        textFieldHost.setPromptText("Host");

        Label labelPort = new Label("Port:");
        labelPort.setPadding(new Insets(5));
        TextField textFieldPort = new TextField("61616");
        textFieldPort.setPromptText("Port");

        Button connectButton = new Button("Connect");

        hBox.getChildren().add(labelCode);
        hBox.getChildren().add(textFieldCode);
        hBox.getChildren().add(labelHost);
        hBox.getChildren().add(textFieldHost);
        hBox.getChildren().add(labelPort);
        hBox.getChildren().add(textFieldPort);
        hBox.getChildren().add(connectButton);

        borderPane.setTop(hBox);

        VBox vBox = new VBox();
        GridPane gridPane = new GridPane();
        HBox hBox2 = new HBox();
        vBox.getChildren().add(gridPane);
        vBox.getChildren().add(hBox2);
        borderPane.setCenter(vBox);

        Label labelTo = new Label("To:");
        TextField textFieldTo = new TextField("C1");
        textFieldTo.setPrefWidth(250);
        Label labelMessage = new Label("Message:");
        TextArea textAreaMessage = new TextArea();
        textAreaMessage.setPrefWidth(250);
        Button sendButton = new Button("Send");
        Label labelImage = new Label("Image");

        File images = new File("images");
        ObservableList<String> observableListImages =
                FXCollections.observableArrayList(images.list());


        ComboBox<String> comboBoxImages = new ComboBox<String>(observableListImages);
        comboBoxImages.getSelectionModel().select(0);
        Button sendImageButton = new Button("Send Image");

        gridPane.setPadding(new Insets(10));
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        textAreaMessage.setPrefRowCount(1);

        gridPane.add(labelTo , 0 , 0);
        gridPane.add(textFieldTo,1,0);
        gridPane.add(labelMessage,0,1);
        gridPane.add(textAreaMessage,1,1);
        gridPane.add(sendButton,2,1);
        gridPane.add(labelImage,0,2);
        gridPane.add(comboBoxImages,1,2);
        gridPane.add(sendImageButton,2,2);

        ObservableList<String> observableListMessages =
                FXCollections.observableArrayList();
        ListView<String> listViewMessages = new ListView<>(observableListMessages);

        File images2 = new File("images/"+comboBoxImages.getSelectionModel().getSelectedItem());
        Image image = new Image(images2.toURI().toString());
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(480);
        imageView.setFitHeight(360);

        hBox2.getChildren().add(listViewMessages);
        hBox2.getChildren().add(imageView);
        hBox2.setPadding(new Insets(10));
        hBox2.setSpacing(10);

        Scene scene = new Scene(borderPane , 1024 , 768);
        primaryStage.setScene(scene);
        primaryStage.show();

        comboBoxImages.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                File images3 = new File("images/"+newValue);
                Image image = new Image(images3.toURI().toString());
                imageView.setImage(image);

            }
        });

        sendButton.setOnAction(event -> {
            try {
                TextMessage textMessage = session.createTextMessage();
                textMessage.setText(textAreaMessage.getText());
                textMessage.setStringProperty("code" ,textFieldTo.getText());
                messageProducer.send(textMessage);
            } catch (JMSException e) {
                e.printStackTrace();
            }

        });

        sendImageButton.setOnAction(event -> {
            try {
                StreamMessage streamMessage = session.createStreamMessage();
                streamMessage.setStringProperty("code",textFieldTo.getText());
                File images4 = new File("images/"+comboBoxImages.getSelectionModel().getSelectedItem());
                FileInputStream fileInputStream = new FileInputStream(images4);
                byte[] data = new byte[(int) images4.length()];
                fileInputStream.read(data);
                streamMessage.writeString(comboBoxImages.getSelectionModel().getSelectedItem());
                streamMessage.writeInt(data.length);
                streamMessage.writeBytes(data);
                messageProducer.send(streamMessage);


            } catch (Exception e) {
                e.printStackTrace();
            }


        });


        connectButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    codeUser = textFieldCode.getText();
                    String host = textFieldHost.getText();
                    int port = Integer.parseInt(textFieldPort.getText());
                    ConnectionFactory connectionFactory =
                            new ActiveMQConnectionFactory("tcp://"+host+":"+port);
                    Connection connection = connectionFactory.createConnection();
                    connection.start();
                    session = connection.createSession(false , Session.AUTO_ACKNOWLEDGE);

                    Destination destination = session.createTopic("bdcc.chat");
                    MessageConsumer messageConsumer = session.createConsumer(destination , "code='"+codeUser+"'");

                    messageProducer = session.createProducer(destination);
                    messageProducer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);


                    //start of function
                    messageConsumer.setMessageListener(new MessageListener() {
                        @Override
                        public void onMessage(Message message) {
                            try {
                                if (message instanceof TextMessage) {
                                    TextMessage textMessage = (TextMessage) message;
                                    observableListMessages.add(textMessage.getText());
                                } else if (message instanceof StreamMessage) {
                                    StreamMessage streamMessage = (StreamMessage) message;
                                    String pictureName = streamMessage.readString();
                                    observableListMessages.add("Picture received : "+pictureName);
                                    int size = streamMessage.readInt();
                                    byte[] data = new byte[size];
                                    streamMessage.readBytes(data);
                                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                                    Image image = new Image(byteArrayInputStream);
                                    imageView.setImage(image);

                                }
                            } catch (JMSException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    //end of function
                    hBox.setDisable(true);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
