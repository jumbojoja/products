<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%; ">
        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">
            <el-input
            v-model="goodsToSearch"
            style=" width: 1000px; margin-left: 200px; margin-right: 120px;"
            placeholder="查询商品"
            class="input-with-select"
            >
                <template #prepend>
                    <el-select v-model="select" placeholder="品类" style="width: 115px">
                    <el-option label="数码产品" value="1" />
                    <el-option label="男装" value="2" />
                    <el-option label="女装" value="3" />
                    <el-option label="男鞋" value="4" />
                    <el-option label="女鞋" value="5" />
                    <el-option label="零食" value="6" />
                    <el-option label="玩具" value="7" />
                    <el-option label="药品" value="8" />
                    <el-option label="图书" value="9" />
                    <el-option label="二手" value="10" />
                    <el-option label="生鲜" value="11" />
                    <el-option label="个护化妆" value="12" />
                    <el-option label="五金机电" value="13" />
                    </el-select>
                </template>
                <template #append>
                    <el-button :icon="Search" @click="SearchGoods"></el-button>
                </template>
            </el-input>

            <span style="margin-left: -10px;">
                <el-button type="primary" @click="loginVisable = true">登录</el-button>
                <el-button type="primary" @click="newUserVisable = true">注册</el-button>
                <el-button type="primary" @click="collectsVisable = true, SearchCollects()">收藏夹</el-button>
            </span>
        </div>    

        <!-- 商品表格 -->
        <el-table v-if="isShow" :data="fitlerTableData" height="600"
            :default-sort="{ prop: 'price', order: 'ascending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 92vw;" highlight-current-row>
            <el-table-column label="照片" prop="imgUrl">
                <template v-slot="scope">
                    <img :src="scope.row.imgUrl" alt="图片" width="90" height="90">
                </template>
            </el-table-column>
            <el-table-column label="商品链接" prop="goodsLink, goodsName">
                <template v-slot="scope">
                    <el-link :href="scope.row.goodsLink" target="_blank" class="buttonText">{{scope.row.goodsName}}</el-link>
                </template>
            </el-table-column>
            <el-table-column prop="platform" label="平台" sortable />
            <el-table-column prop="price" label="价格" sortable />
            <el-table-column label="历史价格">
                <template v-slot="scope">
                    <el-button type="info" v-model="scope.row.skuId"
                    @click="this.priceToSearch = scope.row.skuId, this.goods_history_price = true, SearchPrice()"> 
                        <el-icon>
                            <TrendCharts />
                        </el-icon>
                    </el-button>
                </template>
            </el-table-column>
            <el-table-column label="收藏">
                <template v-slot="scope">
                    <el-button type="warning" v-model="scope.row.skuId"
                    @click="this.collectToAdd = scope.row.skuId, this.collect_goods_visable = true"> 
                        <el-icon>
                            <StarFilled />
                        </el-icon>
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <!-- 新建用户对话框 -->
        <el-dialog v-model="newUserVisable" title="用户注册" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                用户名：
                <el-input v-model="newUserInfo.user_name" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                密码：
                <el-input v-model="newUserInfo.password" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                电子邮件：
                <el-input v-model="newUserInfo.email" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="newUserVisable = false">取消</el-button>
                    <el-button type="primary" @click="adduser"
                        :disabled="newUserInfo.user_name.length === 0 || newUserInfo.password.length <= 8 || newUserInfo.email.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 用户登录对话框 -->
        <el-dialog v-model="loginVisable" title="用户登录" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                用户名：
                <el-input v-model="newUserInfo.user_name" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                密码：
                <el-input v-model="newUserInfo.password" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                电子邮件：
                <el-input v-model="newUserInfo.email" style="width: 12.5vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="newUserVisable = false">取消</el-button>
                    <el-button type="primary" @click="checkuser"
                        :disabled="newUserInfo.user_name.length === 0 || newUserInfo.password.length <= 8 || newUserInfo.email.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 商品历史价格对话框 -->
        <el-dialog v-model="goods_history_price" width="50%" align-center>

            <div v-if="history_goods.length > 0" ref="echart" style="height: 400px; width: 100%; margin: 20px;"></div>

        </el-dialog>

        <!-- 收藏商品对话框 -->  
        <el-dialog v-model="collect_goods_visable" title="收藏商品" width="30%">
            <span>确定收藏此商品？</span>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="collect_goods_visable = false">取消</el-button>
                    <el-button type="warning" @click="ConfirmCollectGoods">
                        收藏
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 用户收藏商品对话框 -->
        <el-dialog v-model="collectsVisable" title="用户收藏" width="80%" align-center>
            <el-table v-if="isShow" :data="fitlerCollectData" height="600"
                :default-sort="{ prop: 'price', order: 'ascending' }" :table-layout="'auto'"
                style="width: 90%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 92vw;" highlight-current-row>
                <el-table-column label="照片" prop="imgUrl">
                    <template v-slot="scope">
                        <img :src="scope.row.imgUrl" alt="图片" width="90" height="90">
                    </template>
                </el-table-column>
                <el-table-column label="商品链接" prop="goodsLink, goodsName">
                    <template v-slot="scope">
                        <el-link :href="scope.row.goodsLink" target="_blank" class="buttonText">{{scope.row.goodsName}}</el-link>
                    </template>
                </el-table-column>
                <el-table-column prop="platform" label="平台" sortable />
                <el-table-column prop="price" label="价格" sortable />
            </el-table>
        </el-dialog>

    </el-scrollbar>
</template>

<script>
import { Check, Delete, Edit, Message, Search, Star } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

import { ref, onMounted } from 'vue'
import { watch, nextTick } from 'vue';
import axios from 'axios'
import * as echarts from 'echarts'

export default {
    data() {
        return {
            isShow: true, // 图书表格展示状态
            tableData: [{ // 列表项
                goodsId: 1,
                price: 11999.0,
                imgUrl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                goodsLink: 'https://item.taobao.com/item.htm?priceTId=2150400417354720569504482ee7eb&utparam=%7B%22aplus_abtest%22%3A%2217cffc8bd67661cb8119ac07867882c1%22%7D&id=733463182856&ns=1&abbucket=14&xxc=taobaoSearch&skuId=5667505824822',
                goodsName: 'Apple/苹果 iPhone 16 Pro Max（A3297）512GB 原色钛金属 支持移动联通电信5G 双卡双待手机',
                platform: '京东商城',
                skuId: '123'
            }, {
                goodsId: 2,
                price: 499.0,
                imgUrl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                goodsLink: 'https://item.taobao.com/item.htm?priceTId=2150400417354720569504482ee7eb&utparam=%7B%22aplus_abtest%22%3A%22b0c894f527150d75d493bd1d5dd8750b%22%7D&id=827280200184&ns=1&abbucket=14&xxc=taobaoSearch&skuId=5553033081567',
                goodsName: '小米（MI）Redmi 12C Helio G85 性能芯 5000万高清双摄 5000mAh长续航 4GB+128GB 深海蓝 智能手机小米红米',
                platform: '天猫',
                skuId: '234'
            }
            ],
            chartInstance: null, // 保存图表实例
            history_goods: [{ // 列表项
                goodsId: 1,
                price: 500.0,
                imgUrl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                goodsLink: 'https://item.taobao.com/item.htm?priceTId=2150400417354720569504482ee7eb&utparam=%7B%22aplus_abtest%22%3A%2217cffc8bd67661cb8119ac07867882c1%22%7D&id=733463182856&ns=1&abbucket=14&xxc=taobaoSearch&skuId=5667505824822',
                goodsName: 'Apple/苹果 iPhone 16 Pro Max（A3297）512GB 原色钛金属 支持移动联通电信5G 双卡双待手机',
                platform: '京东商城',
                skuId: '123'
            }, {
                goodsId: 2,
                price: 499.0,
                imgUrl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                goodsLink: 'https://item.taobao.com/item.htm?priceTId=2150400417354720569504482ee7eb&utparam=%7B%22aplus_abtest%22%3A%22b0c894f527150d75d493bd1d5dd8750b%22%7D&id=827280200184&ns=1&abbucket=14&xxc=taobaoSearch&skuId=5553033081567',
                goodsName: '小米（MI）Redmi 12C Helio G85 性能芯 5000万高清双摄 5000mAh长续航 4GB+128GB 深海蓝 智能手机小米红米',
                platform: '天猫',
                skuId: '234'
            }, {
                goodsId: 3,
                price: 469.0,
                imgUrl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                goodsLink: 'https://item.taobao.com/item.htm?priceTId=2150400417354720569504482ee7eb&utparam=%7B%22aplus_abtest%22%3A%22b0c894f527150d75d493bd1d5dd8750b%22%7D&id=827280200184&ns=1&abbucket=14&xxc=taobaoSearch&skuId=5553033081567',
                goodsName: '小米（MI）Redmi 12C Helio G85 性能芯 5000万高清双摄 5000mAh长续航 4GB+128GB 深海蓝 智能手机小米红米',
                platform: '天猫',
                skuId: '234'
            }, {
                goodsId: 4,
                price: 398.0,
                imgUrl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                goodsLink: 'https://item.taobao.com/item.htm?priceTId=2150400417354720569504482ee7eb&utparam=%7B%22aplus_abtest%22%3A%22b0c894f527150d75d493bd1d5dd8750b%22%7D&id=827280200184&ns=1&abbucket=14&xxc=taobaoSearch&skuId=5553033081567',
                goodsName: '小米（MI）Redmi 12C Helio G85 性能芯 5000万高清双摄 5000mAh长续航 4GB+128GB 深海蓝 智能手机小米红米',
                platform: '天猫',
                skuId: '234'
            }
            ],
            collectData: [{ // 列表项
                goodsId: 1,
                price: 11999.0,
                imgUrl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                goodsLink: 'https://item.taobao.com/item.htm?priceTId=2150400417354720569504482ee7eb&utparam=%7B%22aplus_abtest%22%3A%2217cffc8bd67661cb8119ac07867882c1%22%7D&id=733463182856&ns=1&abbucket=14&xxc=taobaoSearch&skuId=5667505824822',
                goodsName: 'Apple/苹果 iPhone 16 Pro Max（A3297）512GB 原色钛金属 支持移动联通电信5G 双卡双待手机',
                platform: '京东商城',
                skuId: '123'
            }, {
                goodsId: 2,
                price: 499.0,
                imgUrl: 'https://img11.360buyimg.com/n7/jfs/t1/228245/17/27667/67492/66f8b39fF47b5ff80/684a131d6bf6dc91.jpg',
                goodsLink: 'https://item.taobao.com/item.htm?priceTId=2150400417354720569504482ee7eb&utparam=%7B%22aplus_abtest%22%3A%22b0c894f527150d75d493bd1d5dd8750b%22%7D&id=827280200184&ns=1&abbucket=14&xxc=taobaoSearch&skuId=5553033081567',
                goodsName: '小米（MI）Redmi 12C Helio G85 性能芯 5000万高清双摄 5000mAh长续航 4GB+128GB 深海蓝 智能手机小米红米',
                platform: '天猫',
                skuId: '234'
            }
            ],
            goodsToSearch: '', // 待搜索内容(对查询到的结果进行搜索)
            priceToSearch: '',
            collectToAdd: '',
            toSearch: '', // 待搜索内容(对查询到的结果进行搜索)
            Delete,
            Edit,
            Search,
            newBookVisable: false, // 书入库对话框可见性
            incBookVisable: false, // 修改库存对话框可见性
            goods_history_price: false, // 修改书信息对话框可见性
            addBookVisable: false, // 批量入库对话框可见性
            borrowBookVisable: false, // 借书对话框可见性
            returnBookVisable: false, // 还书对话框可见性
            collect_goods_visable: false, // 删除书对话框可见性
            newUserVisable: false, // 新建用户对话框可见性
            loginVisable: false, // 用户登录对话框可见性
            collectsVisable: false,
            newUserInfo: { // 待新建用户信息
                user_name: '',
                password: '',
                email: '',
            },
            loginInfo: { // 登录用户信息
                user_id: '',
                user_name: '',
                password: '',
                email: '',
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
            fileContent: ''
        }
    },
    computed: {
        fitlerTableData() { // 搜索规则
            return this.tableData.filter(
                (tuple) =>
                    (this.toSearch == '') || // 搜索框为空，即不搜索
                    tuple.skuId == this.toSearch || // 商品ID与搜索要求一致
                    tuple.goodsName.includes(this.toSearch) ||
                    tuple.price.toString().includes(this.toSearch)
            )
        },
        fitlerCollectData() { // 搜索收藏规则
            return this.collectData.filter(
                (tuple) =>
                    (this.toSearch == '') || // 搜索框为空，即不搜索
                    tuple.skuId == this.toSearch || // 商品ID与搜索要求一致
                    tuple.goodsName.includes(this.toSearch) ||
                    tuple.price.toString().includes(this.toSearch)
            )
        }
    },
    methods: {
        initChart() {
            // 获取图表容器
            const chartElement = this.$refs.echart;
            // 初始化echarts实例
            this.chartInstance = echarts.init(chartElement);

            // 设置图表的配置项
            const option = {
                title: {
                    text: '商品历史价格折线图',
                },
                tooltip: {
                    trigger: 'axis',
                },
                xAxis: {
                    type: 'category',
                    data: this.history_goods.map((_, index) => `${index + 1}`), // 设置 x 轴为自增的日期或时间点（这里只是简单的“日期”作为示例）
                },
                yAxis: {
                    type: 'value',
                    axisLabel: {
                        formatter: '{value} 元', // 显示价格单位
                    },
                },
                series: [
                    {
                        name: '历史价格',
                        type: 'line', // 设置为折线图
                        data: this.history_goods.map(item => item.price), // 使用商品历史价格数据
                        smooth: true, // 平滑曲线
                        lineStyle: {
                            color: '#ff7f50', // 设置折线颜色
                        },
                        symbolSize: 8, // 设置折线数据点的大小
                    },
                ],
            };

            // 使用配置项填充图表
            this.chartInstance.setOption(option);

            // 窗口大小变化时，自动调整图表大小
            window.addEventListener('resize', this.chartInstance.resize);
        },
        // 更新图表数据
        updateChart() {
            if (this.chartInstance) {
                const option = this.chartInstance.getOption(); // 获取当前图表配置
                option.xAxis[0].data = this.tableData.map(item => item.goodsName);
                option.series[0].data = this.tableData.map(item => item.price);
                this.chartInstance.setOption(option); // 更新图表配置
            }
        },
        SearchGoods() {
            this.tableData = [] // 清空列表
            /* this.toBase64(); */
            axios.post("/search",
                { // 请求体
                    goodsToSearch: this.goodsToSearch
                },
                { 
                    // 设置请求头，明确指定字符集为 UTF-8
                    headers: {
                        "Content-Type": "application/json; charset=UTF-8"
                    }
                })
                .then(response => {
                    let tableData = response.data // 接收响应负载
                    tableData.forEach(goods => { // 对于每个商品
                        this.tableData.push(goods) // 将其加入到列表中
                    })
                })
        },
        SearchPrice() {
            this.history_goods = [] // 清空列表
            axios.post("/pricesearch",
                { // 请求体
                    sku_id: this.priceToSearch
                },
                { 
                    // 设置请求头，明确指定字符集为 UTF-8
                    headers: {
                        "Content-Type": "application/json; charset=UTF-8"
                    }
                })
                .then(response => {
                    let history_goods = response.data // 接收响应负载
                    history_goods.forEach(goods => { // 对于每个商品
                        this.history_goods.push(goods) // 将其加入到列表中
                    })
                    this.$nextTick(() => {
                        this.initChart();  // 初始化图表
                    });
                })
        },
        adduser() {
            // 发出POST请求
            axios.post("/adduser",
                { // 请求体
                    user_name: this.newUserInfo.user_name,
                    password: this.newUserInfo.password,
                    email: this.newUserInfo.email
                })
                .then(response => {
                    if (response.data == "1") {
                        ElMessage.success("注册成功") // 显示消息提醒
                    } else {
                        ElMessage.info("注册失败")
                    }
                    this.newUserVisable = false // 将对话框设置为不可见
                })
        },
        checkuser() {
            // 发出POST请求
            axios.post("/checkuser",
                { // 请求体
                    user_name: this.newUserInfo.user_name,
                    password: this.newUserInfo.password,
                    email: this.newUserInfo.email
                })
                .then(response => {
                    if (response.data == "0") {
                        ElMessage.info("登录失败") // 显示消息提醒
                    } else {
                        ElMessage.success("登录成功")
                        this.loginInfo.user_id = response.data.userId,
                        this.loginInfo.user_name = response.data.userName,
                        this.loginInfo.password = response.data.password,
                        this.loginInfo.email = response.data.email
                        console.log(this.loginInfo)
                    }
                    this.loginVisable = false // 将对话框设置为不可见
                })
        },
        ConfirmCollectGoods() {
            // 发出POST请求
            axios.post("/addcollect",
                { // 请求体
                    user_id: this.loginInfo.user_id,
                    sku_id: this.collectToAdd
                })
                .then(response => {
                    if (response.data == "1") {
                        ElMessage.success("收藏商品成功") // 显示消息提醒
                    } else {
                        ElMessage.info("收藏商品失败")
                    }
                    this.collect_goods_visable = false // 将对话框设置为不可见
                })
        },
        SearchCollects() {
            this.collectData = [] // 清空列表
            axios.post("/searchcollect",
                { // 请求体
                    user_id: this.loginInfo.user_id
                },
                { 
                    // 设置请求头，明确指定字符集为 UTF-8
                    headers: {
                        "Content-Type": "application/json; charset=UTF-8"
                    }
                })
                .then(response => {
                    let collectData = response.data // 接收响应负载
                    collectData.forEach(goods => { // 对于每个商品
                        this.collectData.push(goods) // 将其加入到列表中
                    })
                    ElMessage.info("您收藏的商品无降价")
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
    watch: {
        // 监听对话框的显示状态
        goods_history_price(newVal) {
        if (newVal) {
            // 使用 nextTick 确保对话框已完全渲染
            nextTick(() => {
            this.initChart();
            });
        } else {
            // 关闭对话框时销毁图表实例（可选）
            if (this.chartInstance) {
            this.chartInstance.dispose();
            }
        }
        },
    },
    mounted() { // 当页面被渲染时
    }
}
</script>