import React, { useEffect, useState } from 'react'
import {EmptyState, LegacyCard} from '@shopify/polaris';
import settingFunctions from '../module';
import func from '@/util/func';
import IntegrationsLayout from './IntegrationsLayout';
import PasswordTextField from '../../../components/layouts/PasswordTextField';

function TokensLayout(props) {
  const [tokenList , setTokenList] = useState([])
  async function getTokenList (){
    let arr = await settingFunctions.getTokenList(props.type)
    setTokenList(arr)
  }
  
  useEffect(() =>{
    getTokenList()
  },[])

  const deleteToken = async(id) => {
    await settingFunctions.deleteToken(id)
    getTokenList()
  }

  const seeWork = () => {
    window.open(props.docsUrl)
  }

  const generateNewToken = async() => {
    let arr = await settingFunctions.getNewToken(props.type)
    setTokenList(arr)
  }

  const listComponent = (
    tokenList.map((item,index) =>(
      <LegacyCard.Section title={`Token ${index + 1}`} key={index} 
        actions={[{ content: 'Delete', destructive: true, onAction: () => deleteToken(item.id)}]}>
        <p>{func.prettifyEpoch(item.timestamp)}</p>
        <PasswordTextField field={item.key} />
      </LegacyCard.Section>     
    ))
  )
  
  const emptyComponent = (
      <LegacyCard.Section>
        <EmptyState
          heading='No tokens found'
          action={{content: 'Generate Token',onAction: generateNewToken}}
          // secondaryAction={{
          //   content: 'Learn more',
          // }}
        >
          <p>A token is required to use this integration. Click "Generate token" to generate one.</p>
        </EmptyState>
      </LegacyCard.Section>
  )
  
  const BurpSuiteCard = (
    <LegacyCard title="Tokens" 
        secondaryFooterActions={tokenList.length > 0 ? [{content: 'See how it works', onAction: seeWork}] : []}
        primaryFooterAction={tokenList.length > 0 ? {content: 'Generate token', onAction: generateNewToken} : null}
    >
        {tokenList.length > 0 ? listComponent : emptyComponent}
    </LegacyCard>
  )
  return (
    <IntegrationsLayout title= {props.title} cardContent={props.cardContent} component={BurpSuiteCard} docsUrl={props.docsUrl}/> 
  )
}

export default TokensLayout