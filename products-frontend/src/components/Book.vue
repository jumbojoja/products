<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%; ">
        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">
            <!-- <el-input v-model="toSearch" :prefix-icon="Search" placeholder="查询"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right; ;"
                clearable /> -->
            <el-input
            v-model="toSearch"
            style=" width: 1000px; margin-left: 200px; margin-right: 120px;"
            placeholder="查询商品"
            class="input-with-select"
            >
                <template #prepend>
                    <el-select v-model="select" placeholder="Select" style="width: 115px">
                    <el-option label="Restaurant" value="1" />
                    <el-option label="Order No." value="2" />
                    <el-option label="Tel" value="3" />
                    </el-select>
                </template>
                <template #append>
                    <el-button :icon="Search" />
                </template>
            </el-input>

            <span style="margin-left: 40px;">
                <el-button type="primary" @click="newBookVisable = true">登录</el-button>
                <el-button type="primary" @click="newBookVisable = true">注册</el-button>
            </span>
        </div>    

        <!-- 图书表格 -->
        <el-table v-if="isShow" :data="fitlerTableData" height="600"
            :default-sort="{ prop: 'price', order: 'ascending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 92vw;" highlight-current-row>
            <el-table-column label="照片" prop="imgurl">
                <template v-slot="scope">
                    <img :src="scope.row.imgurl" alt="图片" width="90" height="90">
                </template>
            </el-table-column>
            <el-table-column label="商品链接" prop="link, name">
                <template v-slot="scope">
                    <el-link :href="scope.row.link" target="_blank" class="buttonText">{{scope.row.name}}</el-link>
                </template>
            </el-table-column>
            <el-table-column prop="platform" label="平台" sortable />
            <el-table-column prop="price" label="价格" sortable />
            <el-table-column label="历史价格">
                <template v-slot="scope">
                    <el-button type="info" v-model="scope.row.book_id"
                    @click="this.modifyBookInfo.book_id = scope.row.book_id, this.modifyBookVisable = true"> 
                        <el-icon>
                            <TrendCharts />
                        </el-icon>
                    </el-button>
                </template>
            </el-table-column>
            <el-table-column label="收藏">
                <template v-slot="scope">
                    <el-button type="warning" v-model="scope.row.book_id"
                    @click="this.removeBookInfo.book_id = scope.row.book_id, this.toremove = scope.row.book_id, this.removeBookVisable = true"> 
                        <el-icon>
                            <StarFilled />
                        </el-icon>
                    </el-button>
                </template>
            </el-table-column>
        </el-table>
        
        <!-- 新建书对话框 -->
        <el-dialog v-model="newBookVisable" title="图书入库" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书类别：
                <el-input v-model="newBookInfo.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书书名：
                <el-input v-model="newBookInfo.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="newBookInfo.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <el-input v-model="newBookInfo.publish_year" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书作者：
                <el-input v-model="newBookInfo.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书价格：
                <el-input v-model="newBookInfo.price" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                初始库存：
                <el-input v-model="newBookInfo.stock" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="newBookVisable = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewBook"
                        :disabled="newBookInfo.category.length === 0 || newBookInfo.title.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 修改书库存对话框 -->
        <el-dialog v-model="incBookVisable" title="修改库存" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                图书序号：
                <el-input v-model="incBookInfo.book_id" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                库存增量：
                <el-input v-model="incBookInfo.deltaStock" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="incBookVisable = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmIncBook"
                        :disabled="incBookInfo.book_id.length === 0 || incBookInfo.deltaStock.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 修改书信息对话框 -->
        <el-dialog v-model="modifyBookVisable" title="用户登录" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                用户名：
                <el-input v-model="modifyBookInfo.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                密码：
                <el-input v-model="modifyBookInfo.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                确认密码：
                <el-input v-model="modifyBookInfo.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                电子邮箱：
                <el-input v-model="modifyBookInfo.publish_year" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                手机号：
                <el-input v-model="modifyBookInfo.author" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="modifyBookVisable = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmModifyBook"
                        :disabled="modifyBookInfo.category.length === 0 || modifyBookInfo.title.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 借书对话框 -->
        <el-dialog v-model="borrowBookVisable" title="借书" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                借书序号：
                <el-input v-model="borrowBookInfo.book_id" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                借书卡号：
                <el-input v-model="borrowBookInfo.card_id" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="borrowBookVisable = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmBorrowBook"
                        :disabled="borrowBookInfo.book_id.length === 0 || borrowBookInfo.card_id.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 还书对话框 -->
        <el-dialog v-model="returnBookVisable" title="还书" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                还书序号：
                <el-input v-model="returnBookInfo.book_id" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                还书卡号：
                <el-input v-model="returnBookInfo.card_id" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="returnBookVisable = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmReturnBook"
                        :disabled="returnBookInfo.book_id.length === 0 || returnBookInfo.card_id.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 删除图书对话框 -->  
        <el-dialog v-model="removeBookVisable" title="删除图书" width="30%">
            <span>确定删除<span style="font-weight: bold;">{{ toremove }}号图书</span>吗？</span>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeBookVisable = false">取消</el-button>
                    <el-button type="danger" @click="ConfirmRemoveBook">
                        删除
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 批量入库图书对话框 -->  
        <el-dialog v-model="addBookVisable" title="图书批量入库" width="30%">
            <input type="file" @change="handleFile">
            <button @click="loadFile">加载文件</button> 
        </el-dialog>

    </el-scrollbar>
</template>

<script>
import { Check, Delete, Edit, Message, Search, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
    data() {
        return {
            isShow: true, // 图书表格展示状态
            tableData: [{ // 列表项
                book_id: 1,
                category: "Philosophy",
                title: "Database System Concepts",
                press: "Press-H",
                publish_year: 2010,
                author: "DaDa",
                price: 11999.0,
                stock: 1,
                imgurl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                link: 'https://element-plus.org',
                name: 'Apple/苹果 iPhone 16 Pro Max（A3297）512GB 原色钛金属 支持移动联通电信5G 双卡双待手机',
                platform: '京东商城'
            }, {
                book_id: 2,
                category: "Phi",
                title: "Database System",
                press: "Press-I",
                publish_year: 2012,
                author: "DaDa",
                price: 499.0,
                stock: 6,
                imgurl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                link: 'https://element-plus.org',
                name: '小米（MI）Redmi 12C Helio G85 性能芯 5000万高清双摄 5000mAh长续航 4GB+128GB 深海蓝 智能手机小米红米',
                platform: '天猫'
            }
            ],
            toSearch: '', // 待搜索内容(对查询到的结果进行搜索)
            toremove: '', // 待删除书籍
            Delete,
            Edit,
            Search,
            newBookVisable: false, // 书入库对话框可见性
            incBookVisable: false, // 修改库存对话框可见性
            modifyBookVisable: false, // 修改书信息对话框可见性
            addBookVisable: false, // 批量入库对话框可见性
            borrowBookVisable: false, // 借书对话框可见性
            returnBookVisable: false, // 还书对话框可见性
            removeBookVisable: false, // 删除书对话框可见性
            newBookInfo: { // 待新建书信息
                category: '',
                title: '',
                press: '',
                publish_year: '',
                author: '',
                price: '',
                stock: ''
            },
            incBookInfo: {
                book_id: '',
                deltaStock: ''
            },
            modifyBookInfo: {
                book_id: '',
                category: '',
                title: '',
                press: '',
                publish_year: '',
                author: '',
                price: ''
            },
            newBooks: [{
                book_id: 1,
                category: "Philosophy",
                title: "Database System Concepts",
                press: "Press-H",
                publish_year: 2010,
                author: "DaDa",
                price: 4.25,
                stock: 1
            }, {
                book_id: 2,
                category: "Phi",
                title: "Database System",
                press: "Press-I",
                publish_year: 2012,
                author: "DaDa",
                price: 4.26,
                stock: 6
            }
            ],
            borrowBookInfo: {
                book_id: '',
                card_id: ''
            },
            returnBookInfo: {
                book_id: '',
                card_id: ''
            },
            removeBookInfo: {
                book_id: ''
            },
            fileContent: ''
        }
    },
    computed: {
        fitlerTableData() { // 搜索规则
            return this.tableData.filter(
                (tuple) =>
                    (this.toSearch == '') || // 搜索框为空，即不搜索
                    tuple.book_id == this.toSearch || // 图书号与搜索要求一致
                    tuple.category.includes(this.toSearch) ||
                    tuple.title.includes(this.toSearch) ||
                    tuple.press.includes(this.toSearch) ||
                    tuple.publish_year.toString().includes(this.toSearch) ||
                    tuple.author.includes(this.toSearch) || 
                    tuple.imgurl.includes(this.toSearch)
            )
        }
    },
    methods: {
        QueryBooks() {
            this.tableData = [] // 清空列表
            let response = axios.get('/book') // 向/card发出GET请求
                .then(response => {
                    let tableData = response.data // 接收响应负载
                    tableData.forEach(book => { // 对于每个借书证
                        this.tableData.push(book) // 将其加入到列表中
                    })
                })
        },
        ConfirmNewBook() {
            // 发出POST请求
            axios.post("/book",
                { // 请求体
                    category: this.newBookInfo.category,
                    title: this.newBookInfo.title,
                    press: this.newBookInfo.press,
                    publish_year: this.newBookInfo.publish_year,
                    author: this.newBookInfo.author,
                    price: this.newBookInfo.price,
                    stock: this.newBookInfo.stock
                })
                .then(response => {
                    if (response.data == "1") {
                        ElMessage.success("书籍入库成功") // 显示消息提醒
                    } else {
                        ElMessage.info("书籍入库失败")
                    }
                    this.newBookVisable = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询书以刷新页面
                })
        },
        ConfirmIncBook() {
            // 发出POST请求
            axios.post("/incbook",
                { // 请求体
                    book_id: this.incBookInfo.book_id,
                    deltaStock: this.incBookInfo.deltaStock
                })
                .then(response => {
                    if (response.data == "1") {
                        ElMessage.success("书籍库存修改成功") // 显示消息提醒
                    } else {
                        ElMessage.info("书籍库存修改失败")
                    }
                    this.incBookVisable = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询书以刷新页面
                })
        },
        ConfirmModifyBook() {
            // 发出POST请求
            axios.post("/modifyBook",
                { // 请求体
                    book_id: this.modifyBookInfo.book_id,
                    category: this.modifyBookInfo.category,
                    title: this.modifyBookInfo.title,
                    press: this.modifyBookInfo.press,
                    publish_year: this.modifyBookInfo.publish_year,
                    author: this.modifyBookInfo.author,
                    price: this.modifyBookInfo.price,
                })
                .then(response => {
                    if (response.data == "1") {
                        ElMessage.success("书籍信息修改成功") // 显示消息提醒
                    } else {
                        ElMessage.info("书籍信息修改失败")
                    }
                    this.modifyBookVisable = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询书以刷新页面
                })
        },
        ConfirmBorrowBook() {
            // 发出POST请求
            axios.post("/borrowbook",
                { // 请求体
                    book_id: this.borrowBookInfo.book_id,
                    card_id: this.borrowBookInfo.card_id
                })
                .then(response => {
                    if (response.data == "1") {
                        ElMessage.success("借书成功") // 显示消息提醒
                    } else {
                        ElMessage.info("借书失败")
                    }
                    this.borrowBookVisable = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询书以刷新页面
                })
        },
        ConfirmReturnBook() {
            // 发出POST请求
            axios.post("/returnbook",
                { // 请求体
                    book_id: this.returnBookInfo.book_id,
                    card_id: this.returnBookInfo.card_id
                })
                .then(response => {
                    if (response.data == "1") {
                        ElMessage.success("还书成功") // 显示消息提醒
                    } else {
                        ElMessage.info("还书失败")
                    }
                    this.returnBookVisable = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询书以刷新页面
                })
        },
        ConfirmRemoveBook() {
            // 发出POST请求
            axios.post("/removebook",
                { // 请求体
                    book_id: this.removeBookInfo.book_id,
                })
                .then(response => {
                    if (response.data == "1") {
                        ElMessage.success("删除图书成功") // 显示消息提醒
                    } else {
                        ElMessage.info("删除图书失败(图书未归还)")
                    }
                    this.removeBookVisable = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询书以刷新页面
                })
        },
        handleFile(event) {
            const file = event.target.files[0]
            const reader = new FileReader()
            reader.onload = (event) => {
                this.fileContent = event.target.result
            }
            reader.readAsText(file)
        },
        loadFile() {
            // 发出POST请求
            axios.post("/addbooks",
                { // 请求体
                    boos: this.fileContent
                })
                .then(response => {
                    if (response.data == "1") {
                        ElMessage.success("书籍批量入库成功") // 显示消息提醒
                    } else {
                        ElMessage.info("书籍批量入库失败")
                    }
                    this.addBookVisable = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询书以刷新页面
                })
        }
    },
    mounted() { // 当页面被渲染时
        /* this.QueryBooks() */ // 查询书
    }
}
</script>