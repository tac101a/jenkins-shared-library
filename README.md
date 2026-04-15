## Cấu hình
Thêm thư viện vào Jenkinsfile:
`@Library('my-shared-lib') _`

## Danh sách API
- `notifySlack(String status)`: Gửi thông báo kết quả build về kênh Slack. Yêu cầu credential ID: `slack-token`.