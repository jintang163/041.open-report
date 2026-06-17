import { useEffect, useRef, useCallback, useState } from 'react'
import { message, Spin } from 'antd'
import { useParams } from 'react-router-dom'
import Toolbar from './components/Toolbar'
import FormulaBar from './components/FormulaBar'
import DataSourcePanel from './components/DataSourcePanel'
import PropertyPanel from './components/PropertyPanel'
import ExpressionEditor from './components/ExpressionEditor'
import ConditionalFormatModal from './components/ConditionalFormatModal'
import ChartConfigModal from './components/ChartConfigModal'
import ChartCanvasOverlay from './components/ChartCanvasOverlay'
import { useDesignerStore } from './store/designer'
import {
  initLuckysheet,
  destroyLuckysheet,
  getSelectedCell,
  getSelectedRange,
  getCellValue,
  getCellFormula,
  setCellFormula,
  setCellCustomData,
  refreshLuckysheet
} from './utils/luckysheet'
import {
  extractTemplateFromLuckysheet,
  templateToLuckysheetData,
  createEmptyTemplate,
  parseTemplate,
  type ReportTemplateData
} from './utils/template'
import { getReportById } from '@/api/report'
import type { DataSourceWithDataSets } from './store/designer'
import type { ChartConfig } from './store/designer'

const DesignerPage: React.FC = () => {
  const { id } = useParams<{ id?: string }>()
  const [loadingTemplate, setLoadingTemplate] = useState(false)
  const [loadedTemplate, setLoadedTemplate] = useState<ReportTemplateData | null>(null)
  const {
    setLuckysheetInstance,
    setSelectedCell,
    setSelectedRange,
    setCellValue,
    setDataSources,
    setConditionalFormats,
    setCharts,
    setTemplateName,
    setTemplateId,
    conditionalFormats,
    charts,
    templateName,
    expressionEditorVisible,
    expressionEditorInitialValue,
    setExpressionEditorVisible
  } = useDesignerStore()

  const luckysheetContainerRef = useRef<HTMLDivElement>(null)
  const isInitialized = useRef(false)

  const mockDataSources: DataSourceWithDataSets[] = [
    {
      id: 1,
      name: '销售数据库',
      type: 'MySQL',
      host: 'localhost',
      port: 3306,
      database: 'sales',
      username: 'root',
      status: 1,
      dataSets: [
        {
          id: 101,
          name: 'sales_order',
          datasourceId: 1,
          datasourceName: '销售数据库',
          sql: 'SELECT * FROM sales_order',
          status: 1,
          fields: [
            { name: 'order_id', type: 'VARCHAR', label: '订单号' },
            { name: 'customer_name', type: 'VARCHAR', label: '客户名称' },
            { name: 'product_name', type: 'VARCHAR', label: '产品名称' },
            { name: 'amount', type: 'DECIMAL', label: '金额' },
            { name: 'quantity', type: 'INT', label: '数量' },
            { name: 'order_date', type: 'DATETIME', label: '订单日期' },
            { name: 'status', type: 'VARCHAR', label: '状态' }
          ]
        },
        {
          id: 102,
          name: 'sales_summary',
          datasourceId: 1,
          datasourceName: '销售数据库',
          sql: 'SELECT product_name, SUM(amount) as total FROM sales_order GROUP BY product_name',
          status: 1,
          fields: [
            { name: 'product_name', type: 'VARCHAR', label: '产品名称' },
            { name: 'total', type: 'DECIMAL', label: '总金额' },
            { name: 'order_count', type: 'INT', label: '订单数' }
          ]
        }
      ]
    },
    {
      id: 2,
      name: '用户数据库',
      type: 'PostgreSQL',
      host: 'localhost',
      port: 5432,
      database: 'users',
      username: 'postgres',
      status: 1,
      dataSets: [
        {
          id: 201,
          name: 'user_info',
          datasourceId: 2,
          datasourceName: '用户数据库',
          sql: 'SELECT * FROM user_info',
          status: 1,
          fields: [
            { name: 'user_id', type: 'INT', label: '用户ID' },
            { name: 'username', type: 'VARCHAR', label: '用户名' },
            { name: 'email', type: 'VARCHAR', label: '邮箱' },
            { name: 'phone', type: 'VARCHAR', label: '手机号' },
            { name: 'register_date', type: 'DATE', label: '注册日期' },
            { name: 'status', type: 'INT', label: '状态' }
          ]
        }
      ]
    }
  ]

  const handleLuckysheetReady = useCallback(() => {
    if (!window.luckysheet || isInitialized.current) return

    try {
      let templateData: ReportTemplateData
      if (loadedTemplate) {
        templateData = loadedTemplate
        if (templateData.charts) {
          setCharts(templateData.charts as ChartConfig[])
        }
        if (templateData.conditionalFormats) {
          setConditionalFormats(templateData.conditionalFormats)
        }
      } else {
        templateData = createEmptyTemplate(templateName)
      }
      const sheetData = templateToLuckysheetData(templateData)

      initLuckysheet('luckysheet-container', {
        data: sheetData,
        title: templateData.name || templateName,
        hook: {
          cellMousedown: () => {},
          cellMouseup: () => {
            updateSelection()
          },
          cellClick: () => {
            updateSelection()
          },
          cellSelected: () => {
            updateSelection()
          },
          rangeSelect: () => {
            updateSelection()
          },
          cellEdit: () => {},
          cellEdited: (row: number, column: number, value: any) => {
            setCellValue(value?.v || value?.f || '')
            updateSelection()
          }
        }
      })

      setLuckysheetInstance(window.luckysheet)
      isInitialized.current = true
      message.success(loadedTemplate ? '报表模板加载成功' : '报表设计器初始化成功')
    } catch (error) {
      console.error('Luckysheet initialization error:', error)
      message.error(loadedTemplate ? '报表模板加载失败' : '报表设计器初始化失败')
    }
  }, [templateName, loadedTemplate, setLuckysheetInstance, setCellValue, setCharts, setConditionalFormats])

  const updateSelection = useCallback(() => {
    const cell = getSelectedCell()
    const range = getSelectedRange()

    setSelectedCell(cell)
    setSelectedRange(range)

    if (cell) {
      const value = getCellValue(cell.row, cell.col)
      const formula = getCellFormula(cell.row, cell.col)
      setCellValue(formula || (value !== null && value !== undefined ? String(value) : ''))
    }
  }, [setSelectedCell, setSelectedRange, setCellValue])

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'copy'
  }

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()

    try {
      const data = e.dataTransfer.getData('application/json')
      if (!data) return

      const fieldData = JSON.parse(data)
      const expression = `\${${fieldData.datasetName}.${fieldData.fieldName}}`

      const cell = getSelectedCell()
      if (!cell) {
        message.warning('请先选择一个单元格')
        return
      }

      setCellFormula(cell.row, cell.col, expression)
      setCellCustomData(cell.row, cell.col, {
        expression,
        dataBinding: {
          type: 'field',
          dataset: fieldData.datasetName,
          field: fieldData.fieldName
        }
      })
      setCellValue(expression)
      refreshLuckysheet()

      message.success(`已插入字段: ${fieldData.datasetName}.${fieldData.fieldName}`)
    } catch (error) {
      console.error('Drop error:', error)
      message.error('字段插入失败')
    }
  }

  useEffect(() => {
    setDataSources(mockDataSources)
  }, [setDataSources])

  useEffect(() => {
    if (!id) return
    const loadTemplate = async () => {
      setLoadingTemplate(true)
      try {
        const template = await getReportById(Number(id))
        setTemplateId(template.id!)
        setTemplateName(template.templateName)

        if (template.templateJson) {
          const parsed = parseTemplate(template.templateJson)
          setLoadedTemplate(parsed)
        }
      } catch (error) {
        console.error('加载报表模板失败:', error)
        message.error('加载报表模板失败')
      } finally {
        setLoadingTemplate(false)
      }
    }
    loadTemplate()
  }, [id, setTemplateId, setTemplateName])

  useEffect(() => {
    if (loadingTemplate) return
    if (id && loadedTemplate === null) return

    const checkLuckysheet = () => {
      if (window.luckysheet) {
        handleLuckysheetReady()
      } else {
        setTimeout(checkLuckysheet, 200)
      }
    }

    const loadLuckysheet = () => {
      if ((window as any).luckysheet) {
        handleLuckysheetReady()
        return
      }

      const existingScript = document.querySelector('script[data-luckysheet="true"]')
      if (existingScript) {
        checkLuckysheet()
        return
      }

      const cssLink = document.createElement('link')
      cssLink.rel = 'stylesheet'
      cssLink.href = 'https://cdn.jsdelivr.net/npm/luckysheet@2.1.13/dist/plugins/css/pluginsCss.css'
      document.head.appendChild(cssLink)

      const cssLink2 = document.createElement('link')
      cssLink2.rel = 'stylesheet'
      cssLink2.href = 'https://cdn.jsdelivr.net/npm/luckysheet@2.1.13/dist/plugins/plugins.css'
      document.head.appendChild(cssLink2)

      const cssLink3 = document.createElement('link')
      cssLink3.rel = 'stylesheet'
      cssLink3.href = 'https://cdn.jsdelivr.net/npm/luckysheet@2.1.13/dist/css/luckysheet.css'
      document.head.appendChild(cssLink3)

      const cssLink4 = document.createElement('link')
      cssLink4.rel = 'stylesheet'
      cssLink4.href = 'https://cdn.jsdelivr.net/npm/luckysheet@2.1.13/dist/assets/iconfont/iconfont.css'
      document.head.appendChild(cssLink4)

      const loadScript = (src: string, onLoad?: () => void) => {
        const script = document.createElement('script')
        script.src = src
        script.async = false
        script.setAttribute('data-luckysheet', 'true')
        if (onLoad) script.onload = onLoad
        document.head.appendChild(script)
      }

      loadScript('https://cdn.jsdelivr.net/npm/luckysheet@2.1.13/dist/plugins/js/plugin.js')
      loadScript('https://cdn.jsdelivr.net/npm/luckysheet@2.1.13/dist/luckysheet.umd.js', checkLuckysheet)
    }

    if (document.readyState === 'complete') {
      loadLuckysheet()
    } else {
      window.addEventListener('load', loadLuckysheet)
    }

    return () => {
      window.removeEventListener('load', loadLuckysheet)
      if (isInitialized.current) {
        try {
          destroyLuckysheet()
        } catch {
        }
        isInitialized.current = false
      }
    }
  }, [handleLuckysheetReady, loadingTemplate, id, loadedTemplate])

  const handleExportTemplate = () => {
    if (!window.luckysheet) {
      message.warning('设计器尚未初始化')
      return
    }

    try {
      const sheetsData = window.luckysheet.getSheet()
      const template = extractTemplateFromLuckysheet(sheetsData, {
        name: templateName,
        conditionalFormats,
        charts
      })

      const jsonStr = JSON.stringify(template, null, 2)
      const blob = new Blob([jsonStr], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${templateName}.json`
      a.click()
      URL.revokeObjectURL(url)
      message.success('模板导出成功')
    } catch (error) {
      console.error('Export error:', error)
      message.error('模板导出失败')
    }
  }

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column', background: '#f5f5f5' }}>
      <Toolbar />
      <FormulaBar />

      <div style={{ flex: 1, display: 'flex', overflow: 'hidden' }}>
        <div
          style={{
            width: 280,
            background: '#fff',
            borderRight: '1px solid #e8e8e8',
            overflow: 'hidden'
          }}
        >
          <DataSourcePanel />
        </div>

        <div
          ref={luckysheetContainerRef}
          style={{ flex: 1, position: 'relative', overflow: 'hidden' }}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
        >
          <div
            id="luckysheet-container"
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              margin: 0,
              padding: 0
            }}
          />
          <ChartCanvasOverlay />
          {!isInitialized.current && (
            <div
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: '#fff',
                zIndex: 1000
              }}
            >
              <Spin tip="正在加载报表设计器..." size="large">
                <div style={{ padding: 50 }} />
              </Spin>
            </div>
          )}
        </div>

        <div
          style={{
            width: 320,
            background: '#fff',
            borderLeft: '1px solid #e8e8e8',
            overflow: 'hidden'
          }}
        >
          <PropertyPanel />
        </div>
      </div>

      <ExpressionEditor />
      <ConditionalFormatModal />
      <ChartConfigModal />

      <button
        onClick={handleExportTemplate}
        style={{ display: 'none' }}
        id="export-template-btn"
      />
    </div>
  )
}

export default DesignerPage
