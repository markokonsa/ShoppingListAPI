package ee.qualitylab.shoppingList.api.utilities;

/**
 * Created by Marko on 24.10.2015.
 */
public class CustomResponse {
    String result;
    int id;

    public CustomResponse(int id,String result){
        this.id = id;
        this.result = result;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
