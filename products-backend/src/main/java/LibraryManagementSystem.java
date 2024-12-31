import entities.User;
import entities.Goods;
import queries.ApiResult;

import java.sql.PreparedStatement;
import java.util.List;

/**
 * Note:
 *      (1) all functions in this interface will be regarded as a
 *          transaction. this means that after successfully completing
 *          all operations in a function, you need to call commit(),
 *          or call rollback() if one of the operations in a function fails.
 *          as an example, you can see {@link LibraryManagementSystemImpl#resetDatabase}
 *          to find how to use commit() and rollback().
 *      (2) for each function, you need to briefly introduce how to
 *          achieve this function and how to solve challenges in your
 *          lab report.
 *      (3) if you don't know what the function means, or what it is
 *          supposed to do, looking to the test code might help.
 */
public interface LibraryManagementSystem {

    ApiResult adduser(String user_name, String password, String email);

    ApiResult checkuser(String user_name, String password, String email);

    ApiResult addgoods(String sku_id, String goods_name, String goods_link, String img_url, double price, String platform);

    ApiResult searchgoods(String sku_id);

    ApiResult addcollect(String sku_id, int user_id);

    ApiResult showCollects(int user_id);

    /* ApiResult modifygoods(int goods_id, int sku_id, String goods_name, String goods_link, String img_url, double price, String platform); */

}
