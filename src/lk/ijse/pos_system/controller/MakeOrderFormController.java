package lk.ijse.pos_system.controller;

import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import com.mysql.cj.conf.PropertyDefinitions;
import com.sun.javafx.charts.Legend;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import lk.ijse.pos_system.DB.DbConnection;
import lk.ijse.pos_system.model.Customer;
import lk.ijse.pos_system.model.Item;
import lk.ijse.pos_system.model.Order;
import lk.ijse.pos_system.model.OrderDetails;
import lk.ijse.pos_system.view.TM.CartTM;

import java.io.IOException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MakeOrderFormController {
    public AnchorPane makeOrderContext;
    public Label lblOrderID;
    public Label lblDate;
    public Label lblTime;
    public JFXComboBox cmbCustomerID;
    public JFXTextField txtName;
    public JFXTextField txtaddress;
    public JFXTextField txtCity;
    public JFXComboBox cmbItemID;
    public JFXTextField txtDiscription;
    public JFXTextField txtQTYOnHand;
    public JFXTextField txtUnitPrice;
    public TextField txtQTY;
    public TextField txtDiscount;
    public TableView<CartTM> tblCart;
    public TableColumn colItemID;
    public TableColumn colDiscription;
    public TableColumn colQTY;
    public TableColumn colUnitPrice;
    public TableColumn colDiscount;
    public TableColumn colTotal;
    public Label lblTotal;



    int cartSelectedRowForRemove = -1;

    public void initialize(){
        colItemID.setCellValueFactory(new PropertyValueFactory<>("itemCode"));
        colDiscription.setCellValueFactory(new PropertyValueFactory<>("discription"));
        colQTY.setCellValueFactory(new PropertyValueFactory<>("QTY"));
        colUnitPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colDiscount.setCellValueFactory(new PropertyValueFactory<>("Discount"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));



        loadDateAndTime();
        setOrderId();

        try {
            loadCustomerIds();
            loadItemIds();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }


        cmbCustomerID.getSelectionModel().selectedItemProperty().
                addListener((observable, oldValue, newValue) -> {
                    try {
                        setCustomerData((String) newValue);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
        cmbItemID.getSelectionModel().selectedItemProperty().
                addListener((observable, oldValue, newValue) -> {
                    try {
                        setItemData((String) newValue);
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
        tblCart.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            cartSelectedRowForRemove = (int) newValue;
        });
    }

    private void setOrderId() {
        try {

            lblOrderID.setText(new OrderController().getOrderId());

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private void loadCustomerIds() throws SQLException, ClassNotFoundException {

        List<String> customerIds = new CustomerController()
                .getCustomerIds();
        cmbCustomerID.getItems().addAll(customerIds);

    }

    private void setCustomerData(String customerId) throws SQLException, ClassNotFoundException {
        Customer c1 = new CustomerController().getCustomer(customerId);
        if (c1 == null) {
            new Alert(Alert.AlertType.WARNING, "Empty Result Set");
        } else {
            txtName.setText(c1.getCustName());
            txtName.setText(c1.getCustName());
            txtaddress.setText(c1.getCustAddress());
            txtCity.setText(c1.getCity());

        }
    }
    private void loadItemIds() throws SQLException, ClassNotFoundException {

        List<String> itemIds = new ItemController()
                .getAllItemIds();
        cmbItemID.getItems().addAll(itemIds);

    }

    private void setItemData(String itemId) throws SQLException, ClassNotFoundException {
        Item i1 = new ItemController().getItem(itemId);
        if (i1 == null) {
            new Alert(Alert.AlertType.WARNING, "Empty Result Set");
        } else {
            txtDiscription.setText(i1.getItenDescription());
            txtQTYOnHand.setText(String.valueOf(i1.getQtyOnHand()));
            txtUnitPrice.setText(String.valueOf(i1.getUnitPrice()));
        }
    }
    private void loadDateAndTime() {
        // load Date
        Date date = new Date();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        lblDate.setText(f.format(date));

        // load Time
        Timeline time = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalTime currentTime = LocalTime.now();
            lblTime.setText(
                    currentTime.getHour() + " : " + currentTime.getMinute() +
                            " : " + currentTime.getSecond()
            );
        }),
                new KeyFrame(Duration.seconds(1))
        );
        time.setCycleCount(Animation.INDEFINITE);
        time.play();
    }

    public void ComfirmOrderOnAction(ActionEvent actionEvent) throws IOException {
        ArrayList<OrderDetails>items=new ArrayList<>();
        double total=0;
        for (CartTM tempTm:obList) {
            total+=tempTm.getTotal();
            items.add(new OrderDetails(tempTm.getItemCode(),tempTm.getQTY(),tempTm.getDiscount(),tempTm.getTotal()));
        }
        Order order=new Order(lblOrderID.getText(),lblDate.getText(), (String) cmbCustomerID.getValue(),
                items);

        if (new OrderController().placeOrder(order)){
            new Alert(Alert.AlertType.CONFIRMATION, "Success").show();
            setOrderId();
        }else{
            new Alert(Alert.AlertType.WARNING, "Try Again").show();
        }

        URL resource = getClass().getResource("../view/OrderCompleteForm.fxml");
        Parent load = FXMLLoader.load(resource);
        makeOrderContext.getChildren().clear();
        makeOrderContext.getChildren().add(load);
    }

    public void clearOnAction(ActionEvent actionEvent) {
        if (cartSelectedRowForRemove==-1){
            new Alert(Alert.AlertType.WARNING, "Please Select a row").show();
        }else{
            obList.remove(cartSelectedRowForRemove);
            calculateCost();
            tblCart.refresh();
        }
    }

    ObservableList<CartTM> obList= FXCollections.observableArrayList();
    public void addToCartOnAction(ActionEvent actionEvent) {

        String discription=txtDiscription.getText();
        int qtyOnHand=Integer.parseInt(txtQTYOnHand.getText());
        double unitPrice=Double.parseDouble(txtUnitPrice.getText());
        int QTYForCustomer=Integer.parseInt(txtQTY.getText());
        double discount=Double.parseDouble(txtDiscount.getText());
        double z=(unitPrice*QTYForCustomer);
        Double total=z-((z/100)*discount);


        if (qtyOnHand<QTYForCustomer){
            new Alert(Alert.AlertType.WARNING,"Invalid QTY").show();
            return;
        }

        CartTM tm=new CartTM((String) cmbItemID.getValue(),discription,QTYForCustomer,unitPrice,discount,total);
        try {
            updateQty((String) cmbItemID.getValue(),QTYForCustomer);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        int rowNumber=isExists(tm);

        if (rowNumber==-1){// new Add
            obList.add(tm);
        }else{
            CartTM temp = obList.get(rowNumber);
            CartTM newTm = new CartTM(
                    temp.getItemCode(),
                    temp.getDiscription(),
                    temp.getQTY()+QTYForCustomer,
                    unitPrice,
                    discount,
                    total+temp.getTotal()
            );

            obList.remove(rowNumber);
            obList.add(newTm);
        }
        tblCart.setItems(obList);
        calculateCost();
    }
    private int isExists(CartTM tm){
        for (int i = 0; i < obList.size(); i++) {
            if (tm.getItemCode().equals(obList.get(i).getItemCode())){
                return i;
            }
        }
        return -1;
    }
    void calculateCost(){
        double ttl=0;
        for (CartTM tm:obList
        ) {
            ttl+=tm.getTotal();
        }
        lblTotal.setText(ttl+" /=");

    }
    private boolean updateQty(String itemCode, int qty) throws SQLException, ClassNotFoundException {
        PreparedStatement stm = DbConnection.getInstance().getConnection()
                .prepareStatement
                        ("UPDATE Item SET QtyOnHand=(qtyOnHand-" + qty
                                + ") WHERE ItemCode='" + itemCode + "'");
        return stm.executeUpdate()>0;
    }
}
