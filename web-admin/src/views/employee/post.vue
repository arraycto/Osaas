<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="el-icon-edit" @click="handleCreate">
        新增
      </el-button>

    </div>
    <el-table
      :key="tableKey"
      v-loading="listLoading"
      :data="list"
      border
      fit
      highlight-current-row
      style="width: 100%;"
      @sort-change="sortChange"
    >
      <el-table-column label="序号" type="index" align="center" width="100" />
      <el-table-column label="职称名称" width="300" header-align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="300" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.createTime | parseTime('{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="400" class-name="small-padding fixed-width">
        <template slot-scope="{row}">
          <el-button type="primary" size="mini" @click="handleUpdate(row)">
            编辑
          </el-button>

          <el-button type="danger" size="mini" @click="handleDelete(row)">
            删除
          </el-button>

        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="listQuery.pageIndex" :limit.sync="listQuery.pageSize" @pagination="getList" />

    <el-dialog :title="textMap[dialogStatus]" :visible.sync="dialogFormVisible" width="70%" @close="handleEditDialogClose">
      <el-form ref="dataForm" :rules="rules" :model="goodsDto.goodsMainQuery" label-position="left" label-width="100px" style="width: 400px; margin-left:50px;" />
      <div v-show="dialogStatus!='view'" slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          取消
        </el-button>
        <el-button type="primary" @click="dialogStatus==='create'?createData():updateData()">
          确定
        </el-button>
      </div>
    </el-dialog>

    <el-dialog :visible.sync="dialogPvVisible" title="Reading statistics">
      <el-table :data="pvData" border fit highlight-current-row style="width: 100%">
        <el-table-column prop="key" label="Channel" />
        <el-table-column prop="pv" label="Pv" />
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button type="primary" @click="dialogPvVisible = false">Confirm</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { fetchPostList } from '@/api/employee'
import Pagination from '@/components/Pagination'
import waves from '@/directive/waves' // waves directive
import { getToken } from '@/utils/auth'

// arr to obj, such as { CN : "China", US : "USA" }

export default {
  name: 'ComplexTable',
  components: { Pagination },
  directives: { waves },
  filters: {
    statusFilter(status) {
      const statusMap = {
        published: 'success',
        draft: 'info',
        deleted: 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    return {
      checkAllShop: false,
      checkAllMerchantUsers: [],
      isIndeterminateShop: true,
      circleUrl: 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      squareUrl: 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png',
      UrlList: ['large', 'medium', 'small'],
      radio: '1',
      tableKey: 0,
      list: null,
      total: 0,
      listLoading: true,
      listQuery: {
        pageIndex: 1,
        pageSize: 20,
        name: undefined,
        mobile: undefined
      },
      importanceOptions: [1, 2, 3],
      uplod_action: process.env.VUE_APP_TEST_BASE_API + '/api/upload/img',
      cdnUrl: process.env.VUE_APP_CDN_URL,
      uplod_header: {
        token: getToken()
      },
      sortOptions: [{ label: 'ID Ascending', key: '+id' }, { label: 'ID Descending', key: '-id' }],
      statusOptions: ['published', 'draft', 'deleted'],
      showReviewer: false,
      temp: {
        id: undefined,
        importance: 1,
        remark: '',
        timestamp: new Date(),
        title: '',
        type: '',
        status: 'published'
      },
      goodsDto: {
        goodsAttributeList: [
          {
            name: '',
            value: ''
          }
        ],
        goodsIssueList: [
          {
            answer: '',
            question: ''
          }
        ],
        goodsMainQuery: {
          briefPicUrl: '',
          categoryId: undefined,
          counterPrice: '',
          goodsBrief: '',
          goodsDesc: '',
          isHot: true,
          isNew: true,
          isRecommend: true,
          keywords: '',
          listPicUrls: '',
          marketPrice: '',
          name: '',
          onSale: true,
          primaryPicUrls: '',
          sellVolume: undefined,
          sortOrder: undefined
        },
        merchantUserList: [],
        shopList: []
      },
      dialogFormVisible: false,
      dialogStatus: '',
      textMap: {
        update: '编辑',
        create: '创建',
        view: '查看'
      },
      dialogPvVisible: false,
      pvData: [],
      rules: {
        name: [{ required: true, message: '请输入商品名', trigger: 'change' }],
        categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
        goodsBrief: [{ required: true, message: '请填写商品简介', trigger: 'change' }],
        goodsDesc: [{ required: true, message: '请填写商品描述', trigger: 'change' }],
        keywords: [{ required: true, message: '请填写关键字', trigger: 'change' }],
        counterPrice: [{ required: true, message: '请输入店铺价格', trigger: 'change' }],
        marketPrice: [{ required: true, message: '请输入市场价格', trigger: 'change' }],
        sellVolume:
            [{ required: true, message: '请输入销量', trigger: 'change' },
              { type: 'number', message: '请输入数字', trigger: 'change' }],
        sortOrder:
            [{ required: true, message: '请输入排序号', trigger: 'change' },
              { type: 'number', message: '请输入数字', trigger: 'change' }]
      },
      downloadLoading: false
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.listLoading = true
      fetchPostList(this.listQuery).then(response => {
        this.list = response.data.results
        this.total = response.data.totalRecord

        // Just to simulate the time of the request
        setTimeout(() => {
          this.listLoading = false
        }, 1.5 * 1000)
      })
    },
    handleEditDialogClose() {
    },
    removeAttribute(item) {
      var index = this.goodsDto.goodsAttributeList.indexOf(item)
      if (index !== -1) {
        this.goodsDto.goodsAttributeList.splice(index, 1)
      }
    },
    addAttribute() {
      this.goodsDto.goodsAttributeList.push({
        name: '',
        value: '',
        key: Date.now()
      })
    },
    removeIssue(item) {
      var index = this.goodsDto.goodsIssueList.indexOf(item)
      if (index !== -1) {
        this.goodsDto.goodsIssueList.splice(index, 1)
      }
    },
    addIssue() {
      this.goodsDto.goodsIssueList.push({
        name: '',
        value: '',
        key: Date.now()
      })
    },
    handleFilter() {
      this.listQuery.pageIndex = 1
      if (this.listQuery.mobile === '') this.listQuery.mobile = undefined
      this.getList()
    },
    handlePictureClick(row) {
      this.dialogImageUrl = row.briefPicUrl
      this.dialogVisible = true
    },
    handleModifyStatus(row, status) {
      this.$message({
        message: '操作Success',
        type: 'success'
      })
      row.status = status
    },
    sortChange(data) {
      const { prop, order } = data
      if (prop === 'id') {
        this.sortByID(order)
      }
    },
    resetTemp() {
      this.temp = {
        id: undefined,
        importance: 1,
        remark: '',
        timestamp: new Date(),
        title: '',
        status: 'published',
        type: ''
      }
    },
    handleCreate() {
    },
    handleUpdate(row) {
    },
    handleDelete(row) {
      this.$notify({
        title: 'Success',
        message: 'Delete Successfully',
        type: 'success',
        duration: 2000
      })
      const index = this.list.indexOf(row)
      this.list.splice(index, 1)
    }
  }
}
</script>
