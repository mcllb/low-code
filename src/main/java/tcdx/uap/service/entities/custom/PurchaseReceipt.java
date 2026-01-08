package tcdx.uap.service.entities.custom;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class PurchaseReceipt implements Serializable {

    public Map initData = new HashMap();
}
