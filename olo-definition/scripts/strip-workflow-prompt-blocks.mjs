import fs from 'fs'
import path from 'path'

const roots = [
  path.resolve('d:/olo/olo-labs/olo-mono/olo-definition/olo-configuration/default'),
  path.resolve('d:/olo/olo-labs/olo-mono/olo-definition/olo-configuration/current-active'),
  path.resolve('d:/olo/olo-labs/olo-mono/olo-definition/olo-configuration/dynamic-graph-creation'),
]

for (const root of roots) {
  if (!fs.existsSync(root)) continue
  for (const file of fs.readdirSync(root).filter((name) => name.endsWith('.json'))) {
    const filePath = path.join(root, file)
    const doc = JSON.parse(fs.readFileSync(filePath, 'utf8'))
    delete doc.prompts
    delete doc.defaultPromptId
    if (doc.metadata?.plannerContext) {
      delete doc.metadata.plannerContext.prompts
      delete doc.metadata.plannerContext.defaultPromptId
      delete doc.metadata.plannerContext.promptTemplate
    }
    fs.writeFileSync(filePath, `${JSON.stringify(doc, null, 2)}\n`)
    console.log('stripped workflow prompts from', path.relative(process.cwd(), filePath))
  }
}
