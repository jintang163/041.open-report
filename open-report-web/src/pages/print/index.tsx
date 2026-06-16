import { useEffect, useState, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import { getEmbedReportConfig, getEmbedReportData, EmbedReportConfig, EmbedReportData } from '@/api/embed'

const PrintPage = () => {
  const [searchParams] = useSearchParams()
  const [config, setConfig] = useState<EmbedReportConfig | null>(null)
  const [data, setData] = useState<EmbedReportData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  const reportId = searchParams.get('id')
  const token = searchParams.get('token') || ''
  const title = searchParams.get('title') || ''

  useEffect(() => {
    if (!reportId) {
      setError('缺少报表ID参数')
      setLoading(false)
      return
    }
    loadReport()
  }, [reportId, token])

  const loadReport = async () => {
    try {
      setLoading(true)
      const id = Number(reportId)

      const params: Record<string, any> = {}
      searchParams.forEach((value, key) => {
        if (!['id', 'token', 'title'].includes(key)) {
          params[key] = value
        }
      })

      const [configRes, dataRes] = await Promise.all([
        getEmbedReportConfig(id, token),
        getEmbedReportData(id, token, params)
      ])

      setConfig(configRes)
      setData(dataRes)
    } catch (err: any) {
      setError(err.message || '加载报表数据失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!loading && (config || error)) {
      const timer = setTimeout(() => {
        window.print()
      }, 500)
      return () => clearTimeout(timer)
    }
  }, [loading, config, error])

  const renderTable = (rows: any[]) => {
    if (!rows || rows.length === 0) {
      return <p style={{ color: '#999', textAlign: 'center', padding: 20 }}>暂无数据</p>
    }

    const headers = Object.keys(rows[0])

    return (
      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 16 }}>
        <thead>
          <tr>
            {headers.map((h) => (
              <th
                key={h}
                style={{
                  border: '1px solid #333',
                  padding: '8px 12px',
                  textAlign: 'left',
                  background: '#f0f0f0',
                  fontWeight: 'bold',
                  fontSize: 13
                }}
              >
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIdx) => (
            <tr key={rowIdx}>
              {headers.map((h) => (
                <td
                  key={h}
                  style={{
                    border: '1px solid #333',
                    padding: '6px 12px',
                    fontSize: 12
                  }}
                >
                  {row[h] ?? ''}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    )
  }

  if (loading) {
    return (
      <div ref={containerRef} style={{ padding: 40, textAlign: 'center', fontFamily: 'sans-serif' }}>
        <p>正在加载打印数据...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div ref={containerRef} style={{ padding: 40, textAlign: 'center', fontFamily: 'sans-serif' }}>
        <p style={{ color: 'red' }}>{error}</p>
      </div>
    )
  }

  const displayTitle = title || config?.templateName || '报表打印'

  return (
    <div ref={containerRef} style={{ padding: '20px 40px', fontFamily: 'SimSun, serif', color: '#000' }}>
      <style>
        {`
          @media print {
            @page {
              size: A4;
              margin: 15mm;
            }
            body {
              -webkit-print-color-adjust: exact;
              print-color-adjust: exact;
            }
          }
        `}
      </style>

      <h1
        style={{
          textAlign: 'center',
          fontSize: 24,
          marginBottom: 8,
          fontWeight: 'bold',
          pageBreakAfter: 'avoid'
        }}
      >
        {displayTitle}
      </h1>

      {config?.description && (
        <p style={{ textAlign: 'center', color: '#666', fontSize: 13, marginTop: 0, marginBottom: 20 }}>
          {config.description}
        </p>
      )}

      {data?.dataSets &&
        Object.entries(data.dataSets).map(([dataSetName, dataSetData]: [string, any]) => {
          const rows = dataSetData?.rows || []
          if (dataSetName !== Object.keys(data.dataSets)[0]) {
            return (
              <div key={dataSetName} style={{ pageBreakBefore: 'always' }}>
                <h3 style={{ fontSize: 16, marginBottom: 8, marginTop: 0 }}>{dataSetName}</h3>
                {renderTable(rows)}
              </div>
            )
          }
          return (
            <div key={dataSetName}>
              {Object.keys(data.dataSets).length > 1 && (
                <h3 style={{ fontSize: 16, marginBottom: 8, marginTop: 0 }}>{dataSetName}</h3>
              )}
              {renderTable(rows)}
            </div>
          )
        })}

      {data?.error && (
        <p style={{ color: 'red', marginTop: 16 }}>数据加载异常: {data.error}</p>
      )}

      <div
        style={{
          marginTop: 40,
          textAlign: 'right',
          fontSize: 12,
          color: '#666',
          borderTop: '1px solid #ddd',
          paddingTop: 10
        }}
      >
        <p style={{ margin: 0 }}>打印时间: {new Date().toLocaleString('zh-CN')}</p>
        {config && <p style={{ margin: 0 }}>报表编号: {config.templateCode}</p>}
      </div>
    </div>
  )
}

export default PrintPage
