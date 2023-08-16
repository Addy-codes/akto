import React, { useState, useEffect } from 'react'
import {
    ClipboardMinor
} from '@shopify/polaris-icons';
import {
    HorizontalStack, Box, LegacyCard,
    Button, Popover, ActionList
} from '@shopify/polaris';
import SampleData from './SampleData';
import func from "@/util/func";

function formatData(data){
    let allKeys = [];
      let seen = {};
      JSON.stringify(data.json, function (key, value) {
          if (!(key in seen)) {
              allKeys.push(key);
              seen[key] = null;
          }
          return value;
      });
      allKeys.sort();
    return (data?.firstLine ? data?.firstLine + "\n\n" : "") + (data?.json ? JSON.stringify(data.json, allKeys, 2) : "");
  }

function SampleDataComponent(props) {

    const { type, sampleData, minHeight, showDiff } = props;
    const [sampleJsonData, setSampleJsonData] = useState({ request: { message: "" }, response: { message: "" } });
    const [popoverActive, setPopoverActive] = useState({});

    useEffect(()=>{
        let parsed;
        try{
          parsed = JSON.parse(sampleData?.message)
        } catch {
          parsed = undefined
        }
        let responseJson = func.responseJson(parsed, sampleData?.highlightPaths)
        let requestJson = func.requestJson(parsed, sampleData?.highlightPaths)
        
        let originalParsed;
        try{
          originalParsed = JSON.parse(sampleData?.originalMessage)
        } catch {
          originalParsed = undefined
        }
        let originalResponseJson = func.responseJson(originalParsed, sampleData?.highlightPaths)
        let originalRequestJson = func.requestJson(originalParsed, sampleData?.highlightPaths)
  
        setSampleJsonData({ 
          request: { message: formatData(requestJson), original: formatData(originalRequestJson), highlightPaths:requestJson?.highlightPaths }, 
          response: { message: formatData(responseJson), original: formatData(originalResponseJson), highlightPaths:responseJson?.highlightPaths },
        })
      }, [sampleData])

    async function copyRequest(reqType, type, completeData) {
        let { copyString, snackBarMessage } = await func.copyRequest(type, completeData)
        if (copyString) {
            navigator.clipboard.writeText(copyString)
            func.setToast(true, false, snackBarMessage)
            setPopoverActive({ [reqType]: !popoverActive[reqType] })
        }
    }

    function getItems(type, data) {
        let items = []

        if (type == "request") {
            if (data.message) {
                items.push({
                    content: 'Copy request as curl',
                    onAction: () => { copyRequest(type, "CURL", data.message) },
                },
                    {
                        content: 'Copy request as burp',
                        onAction: () => { copyRequest(type, "BURP", data.message) },
                    })
            }
            if (data.originalMessage) {
                items.push({
                    content: 'Copy original request as curl',
                    onAction: () => { copyRequest(type, "CURL", data.originalMessage) },
                },
                    {
                        content: 'Copy original request as burp',
                        onAction: () => { copyRequest(type, "BURP", data.originalMessage) },
                    })
            }
        } else {
            if (data.message) {
                items.push({
                    content: 'Copy response',
                    onAction: () => { copyRequest(type, "RESPONSE", data.message) },
                })
            }
            if (data.originalMessage) {
                items.push({
                    content: 'Copy original response',
                    onAction: () => { copyRequest(type, "RESPONSE", data.originalMessage) },
                })
            }
        }

        return items;
    }

    return (

        <Box>
            <LegacyCard.Section flush>
                <Box padding={"2"}>
                    <HorizontalStack padding="2" align='space-between'>
                        {func.toSentenceCase(type)}
                        <Popover
                            zIndexOverride={"600"}
                            active={popoverActive[type]}
                            activator={<Button icon={ClipboardMinor} plain onClick={() => 
                                setPopoverActive({ [type]: !popoverActive[type] })} />}
                            onClose={() => setPopoverActive(false)}
                        >
                            <ActionList
                                actionRole="menuitem"
                                items={getItems(type, sampleData)}
                            />
                        </Popover>
                    </HorizontalStack>
                </Box>
            </LegacyCard.Section>
            <LegacyCard.Section flush>
                <SampleData data={sampleJsonData[type]} minHeight={minHeight || "400px"} showDiff={showDiff} />
            </LegacyCard.Section>
        </Box>
    )

}

export default SampleDataComponent