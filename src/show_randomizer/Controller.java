package show_randomizer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.ResourceBundle;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.bunu.MAL.MAL_API;

import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.geometry.Insets;

public class Controller  implements Initializable {
	
	@FXML private TextField username;
	@FXML private PasswordField pass;
	@FXML private ImageView image;
	@FXML private TextArea synopsis;
	@FXML private Label type;
	@FXML private Label title;
	@FXML private Label episodes;
	@FXML private Label status;
	@FXML private TextField link;
	int showID = -1;
	int showIndex = -1;
	String filePath = "";
	MAL_API xml = null;
	ArrayList<String> links = null;
	String[] values = null;
	boolean fromUserList = false;
	
	public void initialize(URL arg0, ResourceBundle arg1) {
	}
	
	public void randomize() throws SAXException, ParserConfigurationException, FileNotFoundException, IOException{
		try{		
			final ContextMenu contextMenu = new ContextMenu();
			MenuItem copy = new MenuItem("copy");
			copy.setOnAction(e -> {
                ClipboardContent content = new ClipboardContent();
                content.putImage(image.getImage());
                Clipboard.getSystemClipboard().setContent(content);
            });
			
			contextMenu.getItems().add(copy);
			this.image.setOnContextMenuRequested(e -> contextMenu.show(image,e.getScreenX(),e.getScreenY()));
			
			this.randomLink();
			String name = this.links.get(this.showIndex);
			if(name != ""){
				Node show = this.xml.getFirstResult(name);
				this.image.setImage(new Image(this.xml.getImage(show)));
				this.title.setText(this.xml.getTitle(show));
				this.type.setText(this.xml.getType(show));
				this.synopsis.setText(this.xml.getSynopsis(show));
				this.status.setText(this.xml.getStatus(show));
				this.episodes.setText(Integer.toString(this.xml.getNumOfEpisodes(show)));
				this.link.setText(this.xml.getAnimeLink(show));
				this.showID = this.xml.getID(show);
			}
		} catch(Exception e){
			this.errorLogin();
		}
		
		
	}
	
	private void errorLogin(){
		this.username.setDisable(false);
		this.pass.setDisable(false);
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error!");
		alert.setHeaderText("Incorrect username and/or password");
		alert.setContentText("Double check the info of the MAL account");

		alert.showAndWait();
	}
	
	private void noTextError(){
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error!");
		alert.setHeaderText("No text file was loaded");
		alert.setContentText("Please load a text file before trying to randomize or delete");

		alert.showAndWait();
	}
	
	private void userLoadComplete(){
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Action Complete!");
		alert.setHeaderText(null);
		alert.setContentText("Finished loading shows from \"Plan To Watch\" list");

		alert.showAndWait();
	}
	
	private void noShowError(){
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("Error!");
		alert.setHeaderText("Trouble adding show to list");
		alert.setContentText("Please make sure you logged in \nor randomized a show");

		alert.showAndWait();
	}
	
	private void updateComplete(){
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Action Complete!");
		alert.setHeaderText(null);
		alert.setContentText("Your anime list has been updated.");

		alert.showAndWait();
	}
	
	private void deleteComplete(){
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Action Complete!");
		alert.setHeaderText(null);
		alert.setContentText("The entry has been deleted from your list.");

		alert.showAndWait();
	}
	
	private void deleteConfirmation(int status) throws ParserConfigurationException{
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Confirmation Dialog");
		alert.setHeaderText("Deleting Show");
		
		if(status == 1){
			alert.setContentText("Are you sure that you want to delete \nthe show from your MAL list?");
		} else{
			alert.setContentText("Are you sure that you want to delete \nthe show from your text list?");
		}

		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == ButtonType.OK){
		    if(status == 1){
		    	this.xml.deleteAnime(this.showID);
		    } else {
			    	try{
						this.links.remove(this.showIndex);
						try {
							BufferedWriter outputWriter = new BufferedWriter(new FileWriter(this.filePath));
							for(String name: this.links){
								outputWriter.write(name+"\n");
							}
							outputWriter.flush();
							outputWriter.close();
							this.deleteComplete();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch(NullPointerException e){
						this.noTextError();
					}
		    }
		}
	}
	
	private void getInfoFor(){
		// Create the custom dialog.
		Dialog<String[]> dialog = new Dialog<>();
		dialog.setTitle("MAL Show Info");
		dialog.setHeaderText("Enter the values for episode, score, and status");

		ButtonType submit = new ButtonType("Submit", ButtonData.OK_DONE);
		dialog.getDialogPane().getButtonTypes().addAll(submit, ButtonType.CANCEL);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);
		grid.setPadding(new Insets(20, 150, 10, 10));

		TextField episodeNo = new TextField();
		episodeNo.setPromptText("Episode Number");
		TextField scoreNo = new TextField();
		scoreNo.setPromptText("Score out of 10");
		
		//1=watching, 2=completed, 3=onhold, 4=dropped, 6=plantowatch
		ChoiceBox<String> status = new ChoiceBox<String>(FXCollections.observableArrayList("Watching", "Completed", "On Hold","Dropped","Plan to Watch"));
		status.getSelectionModel().selectFirst();
		grid.add(episodeNo, 0, 0);
		grid.add(scoreNo, 0, 1);
		grid.add(status, 0, 2);

		dialog.getDialogPane().setContent(grid);

		dialog.setResultConverter(button -> {
		    if (button == submit) {
		    	String statusNo;
		    	if(status.getSelectionModel().getSelectedIndex() == 0){
		    		statusNo = "1";
		    	} else if (status.getSelectionModel().getSelectedIndex() == 1){
		    		statusNo = "2";
		    	} else if (status.getSelectionModel().getSelectedIndex() == 2){
		    		statusNo = "3";
		    	} else if(status.getSelectionModel().getSelectedIndex() == 3){
		    		statusNo = "4";
		    	} else{
		    		statusNo = "6";
		    	}
		    	
		        String[] values = {episodeNo.getText(),statusNo,scoreNo.getText()};
		        this.values = values;
		        return values;
		    }
		    return null;
		});
		dialog.showAndWait();
	}
	
	public void login(){
		if((this.username.getText().replaceAll("\\s+", "").length() != 0) && (this.pass.getText().replaceAll("\\s+", "").length() != 0)){
			this.xml = new MAL_API(this.username.getText(),this.pass.getText());
			this.username.setDisable(true);
			this.pass.setDisable(true);
		}else{
			this.username.clear();
			this.pass.clear();
		}
		
	}
	
	private void randomLink() throws FileNotFoundException, IOException{
		Random rand = new Random();
		if(this.links == null || this.links.size() == 0){
			this.noTextError();
			return;
		}
		this.showIndex = rand.nextInt(links.size());
	}
	
	public void loadText() throws FileNotFoundException, IOException{
		try{
			this.fromUserList = false;
			FileChooser fileChooser = new FileChooser();
			fileChooser.getExtensionFilters().addAll( new ExtensionFilter("Text File", "*.txt"));
			File selectedFile = fileChooser.showOpenDialog(null);
			this.links = new ArrayList<String>();
			this.filePath = selectedFile.getAbsolutePath();
			try(BufferedReader br = new BufferedReader(new FileReader(this.filePath))){
			    for(String line; (line = br.readLine()) != null; ){
			    	line = line.replaceAll("\\s", "_");
			        this.links.add(line);
			    }
			}
		}catch(NullPointerException e){}
	}
	
	public void loadUserList() throws IOException, ParserConfigurationException, SAXException {
		this.links = xml.getList(6);
		this.fromUserList = true;
		this.userLoadComplete();
	}
	
	public void removeFromFile() throws ParserConfigurationException{
		if(!this.fromUserList && this.links != null) {
			this.deleteConfirmation(2);
		}
	}
	
	public void addToList() throws ParserConfigurationException, IOException{
		if(this.showID == -1){
			this.noShowError();
			return;
		}
		
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Editing Your MAL Anime List");
		alert.setHeaderText(null);
		alert.setContentText("Choose your option.");
		
		ButtonType AddNew = new ButtonType("Add");
		ButtonType Update = new ButtonType("Update");
		ButtonType Delete = new ButtonType("Delete");
		ButtonType Cancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().setAll(AddNew, Update,Delete,Cancel);
		Optional<ButtonType> result = alert.showAndWait();
		
		if (result.get() == AddNew){
		    this.getInfoFor();
		    
		    if(this.values != null){
		    	this.xml.addAnime(this.showID, this.values);
		    	this.updateComplete();
		    }
		    
		} else if (result.get() == Update) {
			this.getInfoFor();
			
			if(this.values != null){
		    	this.xml.updateAnime(this.showID, this.values);
		    	this.updateComplete();
		    }
		} else if (result.get() == Delete){
			this.deleteConfirmation(1);
			this.updateComplete();
			
		} else {
		    return;
		}
	}

}
